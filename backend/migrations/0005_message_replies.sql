/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
ALTER TABLE messages_v2 ADD COLUMN reply_to INTEGER REFERENCES messages_v2(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS messages_v2_reply_to_idx ON messages_v2(reply_to);
