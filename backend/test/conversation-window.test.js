import test from 'node:test';
import assert from 'node:assert/strict';
import { DatabaseSync } from 'node:sqlite';
import { readFileSync, readdirSync } from 'node:fs';
import worker from '../src/worker.js';

test('initial conversation returns the newest window in chronological order', async () => {
  const db = new DatabaseSync(':memory:');
  const migrationDir = new URL('../migrations/', import.meta.url);
  for (const file of readdirSync(migrationDir).filter(x => x.endsWith('.sql')).sort()) db.exec(readFileSync(new URL(file, migrationDir), 'utf8'));
  const a='a'.repeat(32), b='b'.repeat(32), token='1'.repeat(32);
  const hash=Buffer.from(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(token))).toString('hex');
  db.prepare('INSERT INTO users VALUES(?,?,?,?)').run(a,'alice',hash,1);
  db.prepare('INSERT INTO users VALUES(?,?,?,?)').run(b,'bob','external:bob',1);
  db.prepare('INSERT INTO sessions VALUES(?,?,?)').run(hash,a,1);
  for(let i=0;i<205;i++) db.prepare('INSERT INTO messages_v2(sender,recipient,body,created_at) VALUES(?,?,?,?)').run(a,b,`message ${i}`,i);
  const DB={prepare(sql){const statement=db.prepare(sql);let args=[];return{bind(...v){args=v;return this},first(){return statement.get(...args)??null},all(){return{results:statement.all(...args)}},run(){return statement.run(...args)}}}};
  const response=await worker.fetch(new Request(`https://example.test/v2/messages?with=${b}&after=0`,{headers:{authorization:`Bearer ${token}`}}),{DB});
  assert.equal(response.status,200);
  const messages=(await response.json()).messages;
  assert.equal(messages.length,200);
  assert.equal(messages[0].id,6);
  assert.equal(messages.at(-1).id,205);
  db.close();
});
