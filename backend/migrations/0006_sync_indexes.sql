/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
CREATE INDEX IF NOT EXISTS messages_v2_recipient_id_idx ON messages_v2(recipient, id);
CREATE INDEX IF NOT EXISTS invitations_recipient_pending_idx ON invitations(recipient, acknowledged, id);
