"""Static method-entry audit against installed mod JARs, including Forge refmap selectors.
Run from the Core root after building. Does not prove bytecode injection sites, MC classes,
optional absent dependencies or runtime/client behavior. Full detail goes to build/.
"""
import json,re,struct,zipfile
from pathlib import Path
root=Path('.')
version=(root/'VERSION').read_text(encoding='utf8').strip()
def parse(data):
 i=8
 def u2():
  nonlocal i
  n=struct.unpack_from('>H',data,i)[0];i+=2;return n
 def u4():
  nonlocal i
  n=struct.unpack_from('>I',data,i)[0];i+=4;return n
 n=u2();cp=[None]*n;k=1
 while k<n:
  t=data[i];i+=1
  if t==1:
   l=u2();cp[k]=data[i:i+l].decode('utf8',errors='replace');i+=l
  elif t in (7,8,16,19,20):cp[k]=u2()
  elif t in (3,4,9,10,11,12,17,18):i+=4
  elif t in (5,6):i+=8;k+=1
  elif t==15:i+=3
  else:raise ValueError(t)
  k+=1
 u2();this=u2();sup=u2();supername=cp[cp[sup]] if sup else None
 interfaces=u2();i+=2*interfaces
 def members():
  nonlocal i
  out=[]
  for _ in range(u2()):
   u2();name=cp[u2()];desc=cp[u2()];out.append((name,desc))
   for _ in range(u2()):u2();l=u4();i+=l
  return out
 fields=members();methods=members();return set(methods),supername
report={}
for platform,lib,src in [('forge','forge/libs','forge/src/main/java'),('neoforge','libs','src/main/java')]:
 jars=list(Path(lib).rglob('*.jar'));classes={}
 for jar in jars:
  with zipfile.ZipFile(jar) as z:
   for n in z.namelist():
    if n.endswith('.class') and not n.startswith('META-INF/') and not n.startswith('net/minecraft/'):
     classes.setdefault(n[:-6],(jar,n))
 cache={}
 def methods(cls):
  if cls not in classes:return set()
  if cls not in cache:
   jar,n=classes[cls]
   with zipfile.ZipFile(jar) as z:cache[cls]=parse(z.read(n))
  m,sup=cache[cls];return m|methods(sup)
 config=json.loads(Path(src.replace('/java','/resources')+'/ars_conflux.mixins.json').read_text())
 checked=[];missing=[];skipped=[]
 refmap={}
 if platform=='forge':
  with zipfile.ZipFile(f'forge/build/libs/ars-conflux-{version}-mc1.20.1-forge.jar') as z:refmap=json.loads(z.read('ars_conflux.refmap.json'))['mappings']
 for mixin in config['mixins']+config.get('client',[]):
  relative='dev/arsconflux/mixin/'+mixin.replace('.','/')+'.java'
  paths=[Path(src)/relative,Path('common/src/main/java')/relative];p=next((p for p in paths if p.exists()),None)
  if not p:skipped.append([mixin,'source missing']);continue
  text=p.read_text(encoding='utf8');ann=re.search(r'@Mixin\((.*?)\)\s*(?:public|abstract)',text,re.S)
  if not ann:skipped.append([mixin,'annotation parse']);continue
  targets=re.findall(r'"([\w.$]+)"',ann[1])
  for cl in re.findall(r'([\w.]+)\.class',ann[1]):
   imp=re.search(r'import\s+([\w.]+\.'+re.escape(cl.split('.')[0])+r');',text)
   if imp:targets.append(imp[1]+cl[len(cl.split('.')[0]):])
   else:
    hits=[c.replace('/','.') for c in classes if c.endswith('/'+cl.replace('.','$'))]
    if len(hits)==1:targets+=hits
  for target in targets:
   cls=target.replace('.','/')
   if cls not in classes:
    # Nested class imports use dots; find the release name.
    found=[c for c in classes if c.replace('$','/').endswith(cls)]
    if len(found)==1:cls=found[0]
   if cls not in classes:skipped.append([mixin,target]);continue
   meth=methods(cls)
   for expr in re.findall(r'\bmethod\s*=\s*(\{[^}]*\}|"[^"]+")',text,re.S):
    selectors=re.findall(r'"([^"]+)"',expr)
    valid=[]
    for sel in selectors:
     name=sel.split('(')[0];desc=sel[len(name):]
     if name=='<init>':valid.append(any(n=='<init>' and (not desc or d==desc) for n,d in meth))
     else:valid.append(any(n==name and (not desc or d==desc) for n,d in meth))
    checked.append([mixin,target,selectors])
    if not any(valid):
     mappings=refmap.get('dev/arsconflux/mixin/'+mixin.replace('.','/'),{})
     for selector in selectors:
      mapped=mappings.get(selector,'');match=re.search(r'(?:;|^)([^;(]+)(\(.*)',mapped)
      if match and (match[1],match[2]) in meth:valid.append(True)
     if not any(valid):missing.append([mixin,target,selectors])
 report[platform]={'checked_selectors':len(checked),'missing':missing,'skipped':skipped,'checked':checked}
Path('build/mixin-target-audit.json').write_text(json.dumps(report,indent=2),encoding='utf8')
for p,r in report.items():print(p,'checked',r['checked_selectors'],'missing',json.dumps(r['missing']),'skipped',json.dumps(r['skipped']))

