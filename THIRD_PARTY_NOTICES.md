# Third-party notices

Ars Conflux code is licensed under the MIT License in `LICENSE`, copyright 2026 EndXiom. This does not relicense its dependencies.

## Optional-addon Simplified Chinese language supplements

The language resources at `assets/ars_controle/lang/zh_cn.json` and
`assets/ars_additions/lang/zh_cn.json` contain Chinese translations and backfills
of the corresponding addons' language entries. Their original text and existing
translations remain copyright their respective contributors:

- Ars Controle, by Qther and contributors: <https://github.com/Vonr/Ars-Controle>
- Ars Additions, by Jarva and contributors: <https://github.com/Jarva/Ars-Additions>

These two resource files are distributed under LGPL-3.0-only, separately from the
MIT-licensed Conflux code. Translation additions and corrections: copyright 2026
EndXiom. They do not relicense upstream artwork, code or other assets. The
uncombined editable JSON files are included in the source distribution and in
`common/src/main/resources/assets/` in this repository. Users may edit or replace
them through ordinary Minecraft resource packs.

The LGPL-3.0 and incorporated GPL-3.0 texts are supplied in
`licenses/optional-addon-localizations/`. No addon executable classes, textures,
models or sound files are bundled. See `docs/optional-addon-localizations.md` for
the exact inspected versions and scope.

## Gradle wrapper

The Gradle wrapper scripts and JAR retain their original copyright and Apache-2.0 notices. A copy of the wrapper license is provided beside each wrapper JAR in `gradle/wrapper/LICENSE`.

## Runtime dependencies

Ars Nouveau and its dependencies are installed separately and remain under their respective licenses. Optional Ars addons are also installed separately. Their JARs and upstream artwork are not committed or bundled.

The optional Ars Zero 2.0.2 adapter invokes the separately installed mod at runtime. It does not bundle Zero classes or artwork, and it does not replace the official Zero JAR.

The Forge 1.20.1 release includes MixinExtras Forge 0.4.1 as a nested Jar-in-Jar dependency. MixinExtras is copyright LlamaLad7, distributed under the MIT License; its original `LICENSE_MixinExtras` is retained inside the nested JAR. See <https://github.com/LlamaLad7/MixinExtras>.

Iron & Nouveau depends on Ars Conflux. Ars Conflux does not include or depend on Iron's Spells 'n Spellbooks or Goety.
