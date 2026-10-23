# Hosting ReConfig policy on Netlify

The self-contained page is `policy/index.html`. It includes responsive CSS, keyboard focus/skip navigation, a print layout and no external fonts, images, scripts or analytics. Its canonical and Open Graph URL are both `https://duv14.com/policy`.

For an existing duv14.com Netlify site, copy the `policy` folder into that site's published output directory. Preserve its other pages and existing redirects. Do not replace the entire existing website with this policy-only folder. The standalone `docs/site` directory can also be selected as a publish directory for a dedicated deployment preview.

Deploy a preview first and check `/policy` and `/policy/` at desktop and mobile widths. Netlify's static directory handling should serve `policy/index.html`; if the site's routing overrides this, add a specific rewrite before any catch-all:

```text
/policy /policy/index.html 200
```

Then publish on the existing Netlify project connected to `duv14.com`, keeping HTTPS enabled. Confirm that `https://duv14.com/policy` returns this HTML rather than a generic app shell/404 and that both canonical/og:url remain the exact permanent URL. Do not redirect the policy to an unrelated site. No hosting/deployment changes were made during source preparation.

The structured changelog at `/changelogsreconfig` is a separate endpoint; do not point it at this HTML page. The client expects a validated structured feed.

## Maintaining policy version 2

Keep `PRIVACY.md`, `docs/site/policy/index.html` and the packaged `assets/reconfig/privacy-summary.txt` consistent with the shipped behavior. The source currently uses MCHeads for head images, PlayerDB/Ashcon profile fallbacks, and local Spotify-only media metadata. Update these disclosures if the implementation changes.

The notice does not certify legal compliance. Before publishing, the operator should confirm their legally required controller identification/contact details, applicable bases, actual service-provider contracts/international-transfer arrangements, and operational retention settings. These cannot be established from source code. The draft explicitly discloses the absence of automated expiry/account deletion, age verification and parental-consent machinery; a policy alone does not supply those controls.

Official drafting reference: [Irish Data Protection Commission transparency guidance](https://www.dataprotection.ie/en/individuals/know-your-rights/right-be-informed-transparency-article-13-14-gdpr). No jurisdiction-specific compliance guarantee is made.
