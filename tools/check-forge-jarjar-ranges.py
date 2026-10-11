#!/usr/bin/env python3
"""Run Forge's actual JarJarSelector against two release JARs; use cached libs only."""
from pathlib import Path
import argparse
import os
import subprocess

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument("core",type=Path)
parser.add_argument("iron",type=Path)
parser.add_argument("--java",default=str(Path(os.environ["JAVA_HOME"])/"bin/java.exe") if "JAVA_HOME" in os.environ else "java")
parser.add_argument("--cache",type=Path,default=Path(os.environ.get("GRADLE_USER_HOME",Path.home()/".gradle"))/"caches/modules-2/files-2.1")
args=parser.parse_args()
deps=[("net.minecraftforge","JarJarSelector","0.3.19"),("net.minecraftforge","JarJarMetadata","0.3.19"),
      ("org.apache.maven","maven-artifact","3.8.5"),("com.google.code.gson","gson","2.8.7"),
      ("com.google.guava","guava","31.1-jre"),("com.machinezoo.noexception","noexception","1.7.1"),
      ("org.slf4j","slf4j-api","1.7.30"),("org.slf4j","slf4j-simple","1.7.30"),
      ("org.apache.commons","commons-lang3","3.12.0"),("com.google.guava","failureaccess","1.0.1")]
def cached(group,artifact,preferred):
    root=args.cache/group/artifact
    exact=list((root/preferred).rglob(artifact+"-"+preferred+".jar"))
    if exact:return exact[0]
    candidates=sorted(p for p in root.rglob("*.jar") if not any(x in p.name for x in ["-sources","-javadoc"]))
    if not candidates:raise FileNotFoundError(f"Cached dependency missing: {group}:{artifact}")
    return candidates[-1]
cp=os.pathsep.join(str(cached(*d)) for d in deps)
source=Path(__file__).with_name("ForgeJarJarRangeCheck.java")
subprocess.run([args.java,"--class-path",cp,str(source),str(args.core.resolve(strict=True)),str(args.iron.resolve(strict=True))],check=True)
