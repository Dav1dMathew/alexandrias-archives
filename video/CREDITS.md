# Credits

## Music

**Destiny** — Alegend
Source: https://freetouse.com/music
Royalty-free background music.

Used in both video cuts (`public/destiny.mp3`).

> **Attribution is a licence condition.** freetouse.com tracks require this
> credit wherever the video is published — YouTube/Vimeo description, the
> submission document, an end card, or all three. Keep the three lines above
> intact when you publish.

Suggested description block:

```
Music track: Destiny by Alegend
Source: https://freetouse.com/music
Royalty Free Background Music
```

## Photography

Real landmark and archive photography from **Wikimedia Commons**, used in the
60-second `Feature` cut (`public/photos/`). Re-fetch with `python3 fetch_photos.py`.

| File | Source image | Author | Licence |
| --- | --- | --- | --- |
| pisa.jpg | The Duomo and Tower of Pisa at sunrise | MHoser | CC BY-SA 4.0 |
| burj.jpg | Burj Khalifa (16260269606) | Laika ac | CC BY-SA 2.0 |
| taj.jpg | Taj Mahal, Agra, India edit2 | Yann; edited by King of Hearts | CC BY-SA 4.0 |
| colosseum.jpg | Colosseum in Rome, April 2007 | Diliff | CC BY-SA 2.5 |
| blueprint.jpg | 1942 architectural drawing, Madigan Hospital (HABS) | Unknown | Public domain |
| archive.jpg | AM 377 fol. 14v (16th-century manuscript) | Det Kongelige Bibliotek | Public domain |

> **The CC BY-SA images require attribution wherever the video is published.**
> Credit the author, name the licence, and link it. The public-domain images
> carry no obligation, but crediting them costs nothing.

Machine-readable details, including the Commons file page for each image, are in
`public/photos/credits.json`.

## Everything else

All remaining visuals are original to this project:

- Blueprint geometry in the cinematic cut is real output from
  `ai-service/generation/archetypes.py`, exported to
  `src/data/blueprints.json` — the same structures `/aa build` places in game.
- Archetype glyphs, diagrams, layout and typography are drawn in code
  (`src/scenes/`, `src/BlueprintHero.tsx`).
- No stock footage, licensed imagery or third-party assets are used.
