# IG table assets

The tree connectors and type icons used by HL7's own profile table renderer
(`org.hl7.fhir.r4.conformance.ProfileUtilities#generateTable`), which is what produces the
"Differential Table" / "Snapshot Table" views on published FHIR IG sites.

They are downloaded from <https://hl7.org/fhir/R4/> and are part of the FHIR specification
publication, redistributed by every published implementation guide. The renderer reads them
from disk and base64-inlines them into the generated HTML, so nothing is fetched at runtime
and no image URLs are served.

`icon_clipboard_*.png`, `icon_element.png`, `icon_entry_blue.png`, `icon_q_*` and
`tbl_vline_end.png` are transparent stand-ins: they are not published at that location and
belong to questionnaire/clipboard rendering rather than profile tables, but the renderer can
name them, and a missing file would fail the render.
