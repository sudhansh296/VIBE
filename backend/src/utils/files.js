const fs = require('fs');
const path = require('path');
const cfg = require('../config');

/** Looks at the first bytes of a file (never at its name or the type the phone claims). Returns { kind, ext, mime } or null. */
function sniff(buf) {
  if (buf.length > 3 && buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) return { kind: 'photo', ext: 'jpg', mime: 'image/jpeg' };
  if (buf.length > 8 && buf.slice(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) return { kind: 'photo', ext: 'png', mime: 'image/png' };
  if (buf.length > 12 && buf.slice(0, 4).toString('ascii') === 'RIFF' && buf.slice(8, 12).toString('ascii') === 'WEBP') return { kind: 'photo', ext: 'webp', mime: 'image/webp' };
  // MP4 / MOV: the file starts with an "ftyp" box
  if (buf.length > 12 && buf.slice(4, 8).toString('ascii') === 'ftyp') return { kind: 'video', ext: 'mp4', mime: 'video/mp4' };
  return null;
}

/**
 * Length of an MP4/MOV in whole seconds, read from its "mvhd" header (no ffmpeg needed). Returns null if it cannot be read.
 * The length is read on the server on purpose: the phone could say anything.
 */
function mp4DurationSeconds(buf) {
  function find(start, end, wanted) {
    let pos = start;
    while (pos + 8 <= end) {
      let size = buf.readUInt32BE(pos);
      const type = buf.slice(pos + 4, pos + 8).toString('ascii');
      let header = 8;
      if (size === 1) { if (pos + 16 > end) return null; size = Number(buf.readBigUInt64BE(pos + 8)); header = 16; }
      else if (size === 0) size = end - pos;
      if (size < header || pos + size > end) return null;
      if (type === wanted) return { start: pos + header, end: pos + size };
      pos += size;
    }
    return null;
  }
  const moov = find(0, buf.length, 'moov');
  if (!moov) return null;
  const mvhd = find(moov.start, moov.end, 'mvhd');
  if (!mvhd) return null;
  const v = buf[mvhd.start];
  let timescale; let duration;
  if (v === 1) { timescale = buf.readUInt32BE(mvhd.start + 20); duration = Number(buf.readBigUInt64BE(mvhd.start + 24)); }
  else { timescale = buf.readUInt32BE(mvhd.start + 12); duration = buf.readUInt32BE(mvhd.start + 16); }
  if (!timescale) return null;
  return Math.ceil(duration / timescale);
}

/** Saves a buffer under uploads/<relative path> (the folder is created) and returns the relative path. */
function save(rel, buf) {
  const abs = path.join(cfg.uploadDir, rel);
  fs.mkdirSync(path.dirname(abs), { recursive: true });
  fs.writeFileSync(abs, buf);
  return rel;
}

function remove(rel) {
  if (!rel) return;
  fs.rm(path.join(cfg.uploadDir, rel), { force: true }, () => {});
}

const absPath = (rel) => path.join(cfg.uploadDir, rel);

/** What kind of gift animation a file is, from its first bytes: mp4, svga, lottie, webp or gif. Null if none of these. */
function sniffAnimation(buf, name = '') {
  if (buf.length < 12) return null;
  if (buf.slice(4, 8).toString('ascii') === 'ftyp') return { type: 'mp4', ext: 'mp4' };
  if (buf.slice(0, 4).toString('ascii') === 'RIFF' && buf.slice(8, 12).toString('ascii') === 'WEBP') return { type: 'webp', ext: 'webp' };
  if (buf.slice(0, 4).toString('ascii') === 'GIF8') return { type: 'gif', ext: 'gif' };
  // SVGA 1.x is a zip, SVGA 2.x is zlib-compressed protobuf; both are only accepted with the .svga name
  if (/\.svga$/i.test(name) && ((buf[0] === 0x50 && buf[1] === 0x4b) || buf[0] === 0x78)) return { type: 'svga', ext: 'svga' };
  const head = buf.slice(0, 2000).toString('utf8').trimStart();
  if (head.startsWith('{') && /"layers"\s*:/.test(buf.toString('utf8'))) {
    try { JSON.parse(buf.toString('utf8')); return { type: 'lottie', ext: 'json' }; } catch { return null; }
  }
  return null;
}

module.exports = { sniff, sniffAnimation, mp4DurationSeconds, save, remove, absPath };
