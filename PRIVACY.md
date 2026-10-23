# ReConfig Privacy Policy

Policy version 2 · Updated 20 October 2026

Permanent policy: https://duv14.com/policy

## At a glance

ReConfig is maintained by duv14. Local gameplay settings stay on your computer. Online social features send identity, friendship, message and presence data to the ReConfig service. Profiles are private by default; publishing your bio, tag and selected emoji requires a separate explicit choice. Messages are not end-to-end encrypted. ReConfig does not sell personal data or use behavioural advertising.

Privacy contact: therealduv14@gmail.com. Please use email for personal requests rather than posting private details in public GitHub issues.

## What is processed

- Local configuration: module choices, HUD positions, scales and backgrounds, keybinds, theme settings, compatibility preferences, local waypoints, cached profile/skin information, policy acceptance version and social synchronization cursors. These support your preferences and run locally; they are not part of the social database. Local support logs or reports you choose to share can contain some of this information.
- Account identity and authentication: Minecraft username and UUID, a randomly generated ReConfig bearer token sent over HTTPS, its SHA-256 authentication/session hash stored by the Worker, and timestamps. Your local client retains its ReConfig token. Enrollment proof sends your identity and a temporary random serverId to the Worker; Minecraft's own session service uses your Minecraft access token to establish proof. ReConfig's Worker does not receive your Microsoft password or Minecraft access token.
- Profiles: bio, short tag, optional bundled emoji identifier, publication choice, consent timestamp and update timestamp. Published username, UUID, bio, tag and emoji are discoverable by other authenticated ReConfig users. Private profile fields are available to their account owner. Public Minecraft names and skins can still be looked up independently of a private ReConfig profile.
- Friends: requests, sender/recipient identifiers, request timestamps and accepted friendships. A user may look up and request a valid Minecraft account that has not installed ReConfig; the service stores a contact record for that account.
- Messages: sender and recipient identifiers, message text or bundled emoji identifiers, reply references, timestamps, delivery-related state and deletion records. Each conversation participant receives its messages. Deleting your message removes its body from the active message table and records an identifier/timestamp tombstone for synchronization. Recipient copies, screenshots, caches, logs or provider backups may persist.
- Presence: account identifier, last-seen time and a hash of the current server information. Friends can see online/offline status. A hash is not a guarantee of anonymity: a known or guessable server address may be matched against it.
- Server invitations: sender, recipient, full server address, creation time and acknowledgment state. The invited friend receives the address when you explicitly invite them. Do not invite someone to a private server unless you intend to share its address.
- Public server directory: operator-curated public server names, addresses, descriptions, optional icon URLs and optional player counts. Minecraft may contact a listed server for status/ping or when you connect. Such a server receives the normal network and Minecraft connection information. The directory is not built from users' private presence or invitation addresses.
- Technical information and support: hosting providers may receive IP addresses, request metadata, timestamps and operational/security logs. The Worker can log technical errors. Support emails, voluntarily shared logs, screenshots and GitHub reports contain the information you submit; review them and remove tokens and private addresses first.
- Music HUD: the current Windows implementation inspects supported Spotify operating-system media sessions locally for title, artist, playback status and timeline metadata. It does not upload listening activity. This release does not inspect arbitrary browser titles or claim reliable YouTube identification; unsupported YouTube/browser sessions remain inactive. No listening history is sent to the social backend.

## When connections happen

After the policy gate is accepted and the social service starts, background enrollment, presence and lightweight notification synchronization can run during gameplay even when the Friends page is closed. Opening social pages requests fuller state. Profile/skin lookups, directory status checks and changelog fetching can contact their respective services when those features are used. Accepting the general policy does not publish your ReConfig profile.

Closing Minecraft or removing the mod stops its future requests. Avoiding the Friends page alone does not necessarily stop background social requests. Previously stored server records remain until they are removed through supported controls or a deletion request. Declining the policy leaves ReConfig's gated interface unavailable.

## Why data is used and legal bases

ReConfig uses this information to save your preferences, identify accounts, deliver the social features you request, display skins and directory information, prevent duplicate/spam requests, investigate failures and respond to support requests. It does not use social messages for behavioural advertising or automated decisions with legal or similarly significant effects.

Where applicable data-protection law requires a legal basis, the purposes are: consent for optional public-profile publication; providing the requested service where processing is necessary to perform that service; and legitimate interests in operating, securing and troubleshooting the service, subject to users' rights and interests. Information may also be retained or disclosed where a legal obligation requires it. Accepting this notice is not blanket consent to unrelated processing, nor does it waive your rights. You can withdraw publication consent by making your profile private; this does not invalidate prior lawful processing.

## Providers and recipients

- Cloudflare Workers processes social API requests and Cloudflare D1 stores account, profile, friendship, message, presence, invitation and synchronization data. Cloudflare also handles infrastructure and technical metadata.
- Netlify hosts the duv14.com policy and structured changelog content. Visiting those pages or fetching the changelog can expose normal HTTP/network metadata to the host. This self-contained policy page adds no analytics scripts, advertising or tracking pixels.
- Mojang/Microsoft Minecraft Services and the Minecraft session server provide public profile lookups and enrollment ownership checks. Minecraft's normal skin services may deliver skin textures.
- PlayerDB and Ashcon are fallback public Minecraft profile lookup providers used by the Worker when primary lookup services fail. The lookup identifier is sent to them; the service making the request also exposes network metadata.
- MCHeads (mc-heads.net) is used by the current source for player-head images; requests contain the Minecraft username or UUID and expose the requesting client's network metadata. Skin/head caches reduce repeated requests but do not make them anonymous.
- GitHub distributes source and hosts public issue reports, and inherited resource references may use GitHub-hosted assets. Anything posted to a public issue is public and subject to GitHub's policies.
- Other ReConfig users receive data needed for their requests, friendships, conversations and invitations, and can search intentionally published profiles. Public server operators and external icon hosts receive network requests when their services are used.

These services have their own privacy notices and operational policies. ReConfig does not claim to control their independent logging, retention or every onward transfer. ReConfig does not sell your personal data.

## Retention and deletion

Local settings, credentials and caches remain until removed from the relevant Minecraft/configuration directories or operating-system Java preferences. Uninstalling the JAR alone may leave these local files and preferences behind.

The backend currently has no automatic retention-expiry or account-deletion scheduler. Account records, profiles, relationships, messages, request events, invitation records and deletion tombstones can remain until modified or removed through supported controls or operator action. Retention decisions consider whether a record is still needed to provide the account or requested interaction, resolve a support/security issue, maintain synchronization, or meet an applicable legal obligation. There is no implemented fixed deletion deadline to promise.

Making a profile private removes it from public profile discovery; it does not erase the saved bio or account. Removing a friend removes that relationship, not the conversation history. Deleting a sent message removes the active message body but is not a recall of recipients' copies. Provider logs and backups follow provider arrangements; exact deployed retention settings are not specified here. Contact the maintainer for a case-specific explanation or deletion request.

## Security and international processing

Social traffic uses HTTPS, and the database stores hashes of ReConfig bearer tokens rather than their plaintext values. Ownership proof is required for new enrollment and token replacement in this version. Historical registrations are not retroactively verified. Rate limits and route authorization reduce some abuse but do not guarantee security. Keep your local token private and do not send it in a support report.

Messages are not end-to-end encrypted: the service infrastructure and authorized operators can technically access stored message content. No system is perfectly secure. Local settings and Java preferences are not a protected password vault.

Cloudflare, Netlify, Microsoft/Mojang, GitHub and lookup/image providers may process information in countries outside your own, including outside the EEA or UK. Protection and legal access rules can differ. Applicable transfer arrangements depend on the providers and the operator's deployed contracts; this notice does not assert that a particular transfer mechanism or regional-storage guarantee has been independently verified. Ask the maintainer for information about the arrangements relevant to your request.

## Your choices and rights

Email therealduv14@gmail.com with the subject “ReConfig privacy” and your Minecraft username/UUID plus the request you want to make. Never include your bearer token, Microsoft password or Minecraft access token. Reasonable verification may be needed to avoid disclosing or deleting someone else's account. Requests are handled by the maintainer, not an automated deletion system.

Depending on applicable law, you may request access, correction, deletion, restriction, portability, or object to processing, and may withdraw consent where consent is the basis. These rights can have limits, including protecting other people's information and records required by law. You may complain to the data-protection authority where you live or work, or where you believe an infringement occurred. For an explanation of these rights, see the Irish Data Protection Commission's guidance: https://www.dataprotection.ie/en/individuals/rights-individuals-under-general-data-protection-regulation

## Children and younger players

Minecraft is used by younger players. Do not put your real name, school, home address, passwords or other private details in your profile or messages. Public means other users can find and copy it. Speak with a parent or guardian if you are unsure about sharing information.

ReConfig does not implement age verification or a parental-consent workflow. Where parental authorization is required by applicable law, a child should not use online social or public-profile features without the required authorization. A parent or guardian can contact the maintainer about a child's information or request its removal. The policy gate is not an age check or proof of parental consent.

## Updates and contact

When the policy version changes, the mod requires renewed policy acceptance. Publication remains a separate explicit choice. The permanent copy is https://duv14.com/policy and an offline summary is bundled with the mod.

Maintainer: duv14 · Privacy and support: therealduv14@gmail.com · Source and non-private issues: https://github.com/duv14/ReConfig
