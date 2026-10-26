/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Loads the built UI in a DOM against a running service and fails on anything the page throws.
 *
 * This catches what a type check cannot: a hook reaching a binding before it is initialised, a bad
 * dependency array, a render that throws on real data. Loading a route is not enough on its own, because the
 * state that breaks usually only exists after an interaction, so labels passed after the route are clicked in
 * turn.
 *
 *   npm run smoke -- "/packages/hl7.fhir.us.core-6.1.0" "USCorePatientProfile"
 *
 * Point it at another service with SERVICE=http://host:port, and assert on what the page ends up saying with
 * EXPECT="some text" (repeat by separating with |). Throwing is the usual failure, but a panel that quietly
 * stops rendering is not, so the text is worth pinning too.
 */
const { JSDOM } = require('jsdom');
const { TextEncoder, TextDecoder } = require('util');
const fs = require('fs');
const path = require('path');

const dist = path.join(__dirname, 'dist');
const base = process.env.SERVICE || 'http://localhost:9095';
const route = process.argv[2] || '/';
const expected = (process.env.EXPECT || '').split('|').map((s) => s.trim()).filter(Boolean);
const clicks = process.argv.slice(3);

if (!fs.existsSync(dist)) {
  console.error('No dist/ to test. Run `npm run build` first.');
  process.exit(1);
}

const errors = [];
const dom = new JSDOM(fs.readFileSync(path.join(dist, 'index.html'), 'utf8'), {
  url: `${base}/#${route}`,
  runScripts: 'outside-only',
  pretendToBeVisual: true,
});
const { window } = dom;

window.addEventListener('error', (e) => errors.push('error: ' + (e.error?.stack || e.message)));
window.addEventListener('unhandledrejection', (e) => errors.push('rejection: ' + (e.reason?.stack || e.reason)));
window.console.error = (...args) => errors.push('console.error: ' + args.join(' '));

// Real requests to the running service, so the page sees the data it would in a browser.
window.fetch = (input, init) => fetch(String(input).startsWith('http') ? input : base + input, init);
// Globals jsdom does not provide that the bundle expects.
window.TextEncoder = TextEncoder;
window.TextDecoder = TextDecoder;
window.ResizeObserver = window.ResizeObserver || class { observe() {} unobserve() {} disconnect() {} };
window.matchMedia = window.matchMedia || (() => ({
  matches: false, addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {},
}));

const bundleName = fs.readdirSync(path.join(dist, 'assets')).find((file) => file.endsWith('.js'));
try {
  window.eval(fs.readFileSync(path.join(dist, 'assets', bundleName), 'utf8'));
} catch (e) {
  errors.push('bundle threw: ' + (e.stack || e));
}

function clickByText(label) {
  const node = Array.from(window.document.querySelectorAll('p,span,div,button'))
    .find((element) => element.children.length === 0 && element.textContent.trim() === label);
  if (!node) {
    errors.push(`nothing to click labelled "${label}"`);
    return;
  }
  node.click();
}

let step = 0;
function tick() {
  if (step < clicks.length) {
    console.log(`  click: ${clicks[step]}`);
    clickByText(clicks[step]);
    step += 1;
    setTimeout(tick, 4000);
    return;
  }
  report();
}

function report() {
  const text = (window.document.getElementById('root')?.textContent || '').replace(/\s+/g, ' ').trim();
  console.log(`route ${route}: rendered ${text.length} chars`);
  if (text) {
    console.log('  ', text.slice(0, 150));
  }
  expected.forEach((phrase) => {
    const found = text.includes(phrase);
    console.log(`  expect "${phrase}": ${found ? 'found' : 'MISSING'}`);
    if (!found) {
      errors.push(`expected text not on the page: "${phrase}"`);
    }
  });
  if (errors.length) {
    console.log(`  FAILURES (${errors.length}):`);
    errors.slice(0, 3).forEach((e) => console.log('   ', e.split('\n').slice(0, 3).join(' | ')));
    process.exit(1);
  }
  console.log('  no errors');
}

setTimeout(tick, 6000);
