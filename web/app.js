// The page is served by the Java server, so addresses are relative (no "http://localhost:8080" needed)
const API = "";
const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];
const DEPARTMENTS = { CE: "Computer Science and Engineering", DS: "Data Science" };

// One jewel colour per course, so the week is easy to scan
const COLOURS = ["#0f8a63", "#2b4fd0", "#7b4bb7", "#d98a0b", "#0e8fa0", "#6b9b1f", "#4b3fa8", "#d0508f"];
const colourOf = {};

let student = null;       // { username, programme, level } once the student has continued
let pool = [];            // every course of the student's department (all levels)
let shown = [];           // the courses currently listed as tiles
let lastData = null;      // the student's classes from the server
let lastBlocks = [];      // the study blocks from the server
let lastReg = "";         // the registration the timetable was built from
let savedFocus = [];      // hardest courses remembered from last time

// Make text safe to place inside HTML
function esc(text) {
  return String(text == null ? "" : text)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// Send form data to the server and read the JSON answer
async function post(path, fields) {
  const response = await fetch(API + path, { method: "POST", body: new URLSearchParams(fields) });
  const data = await response.json();
  if (data.error) throw new Error(data.error);
  return data;
}

async function get(path) {
  const response = await fetch(API + path);
  const data = await response.json();
  if (data.error) throw new Error(data.error);
  return data;
}

/* ================= welcome page ================= */

document.getElementById("department").addEventListener("change", function () {
  document.body.dataset.dept = this.value;          // switches the colours and artwork
});

async function onContinue(event) {
  event.preventDefault();
  const message = document.getElementById("profileMessage");
  message.textContent = "";

  const username = document.getElementById("username").value.trim();
  const programme = document.getElementById("department").value;
  const level = document.getElementById("level").value;

  let data;
  try {
    data = await post("/api/profile", { username, programme, level });
  } catch (error) {
    message.textContent = error.message === "Failed to fetch"
      ? "Cannot reach the ClassCue server. Is Server.java running?"
      : error.message;
    return;
  }
  try { localStorage.setItem("classcueUser", username); } catch (e) { /* storage may be blocked */ }
  await startApp(data);
}

/* ================= app ================= */

async function startApp(data) {
  student = data.student;
  document.body.dataset.dept = student.programme;
  document.getElementById("welcome").hidden = true;
  document.getElementById("app").hidden = false;
  window.scrollTo(0, 0);

  document.getElementById("greeting").textContent =
    (data.isNew ? "Welcome, " : "Welcome back, ") + student.username;
  document.getElementById("whoChip").textContent =
    DEPARTMENTS[student.programme] + " · Level " + student.level;

  // reset anything left from a previous student
  lastData = null;
  lastBlocks = [];
  document.getElementById("result").hidden = true;
  document.getElementById("prefs").hidden = true;

  const box = document.getElementById("courses");
  box.textContent = "Loading courses...";
  try {
    pool = await get("/api/courses?programme=" + encodeURIComponent(student.programme));
  } catch (error) {
    box.textContent = "Could not load courses: " + error.message;
    return;
  }
  pool.forEach((c, i) => { colourOf[c.code] = COLOURS[i % COLOURS.length]; });

  const savedMap = parseRegistration(data.registration);     // null if nothing saved yet
  shown = pool.filter(c => c.level === student.level || (savedMap && c.code in savedMap));
  renderCourses(savedMap);

  applyPreferences(data.preferences);

  // a returning student sees their timetable (and study plan) straight away
  if (savedMap && shown.length > 0) {
    await buildTimetable(false);
    if (data.preferences && lastData) await buildPlan(false);
  }
}

// "CE 251:,CE 271:A" -> { "CE 251": "", "CE 271": "A" }
function parseRegistration(text) {
  if (!text) return null;
  const map = {};
  for (const part of text.split(",")) {
    const [code, group] = part.split(":");
    map[code] = group || "";
  }
  return map;
}

function renderCourses(savedMap) {
  const box = document.getElementById("courses");
  box.innerHTML = "";
  if (shown.length === 0) {
    box.textContent = "No courses are loaded for this department and level yet.";
  }
  for (const c of shown) {
    const checked = savedMap ? (c.code in savedMap) : true;
    addTile(c, checked, savedMap ? savedMap[c.code] : "");
  }
  refreshCarryChoices();
}

function addTile(course, checked, group) {
  const row = document.createElement("div");
  row.className = "course";
  row.dataset.code = course.code;
  row.style.setProperty("--c", colourOf[course.code] || "#2b4fd0");

  let groupPicker = "";
  if (course.groups.length > 0) {
    const options = course.groups
      .map(g => `<option value="${esc(g)}"${g === group ? " selected" : ""}>Group ${esc(g)}</option>`)
      .join("");
    groupPicker = `<select class="course-group">${options}</select>`;
  }

  const title = course.title ? " " + esc(course.title) : "";
  const credits = (course.credits === null || course.credits === undefined) ? ""
    : ` <small>(${course.credits} credit${course.credits === 1 ? "" : "s"})</small>`;
  row.innerHTML =
    `<input type="checkbox" class="course-check"${checked ? " checked" : ""}>` +
    `<label><b>${esc(course.code)}</b>${title}${credits}</label>` +
    groupPicker;
  // clicking the text ticks the box
  row.querySelector("label").addEventListener("click", () => {
    const box = row.querySelector(".course-check");
    box.checked = !box.checked;
  });
  document.getElementById("courses").appendChild(row);
}

// Courses from the department that are not listed yet (for carry-overs)
function refreshCarryChoices() {
  const select = document.getElementById("carrySelect");
  select.innerHTML = "";
  const others = pool.filter(c => !shown.some(s => s.code === c.code));
  for (const c of others) {
    const option = document.createElement("option");
    option.value = c.code;
    option.textContent = c.code + (c.title ? " " + c.title : "") + " (Level " + c.level + ")";
    select.appendChild(option);
  }
  document.getElementById("carryBox").hidden = others.length === 0;
}

function addCarryOver() {
  const code = document.getElementById("carrySelect").value;
  const course = pool.find(c => c.code === code);
  if (!course) return;
  shown.push(course);
  addTile(course, true, "");
  refreshCarryChoices();
}

// Turn the ticked courses into "CE 251:,CE 271:A,..."
function buildRegistration() {
  const parts = [];
  for (const row of document.querySelectorAll("#courses .course")) {
    if (!row.querySelector(".course-check").checked) continue;
    const select = row.querySelector(".course-group");
    parts.push(row.dataset.code + ":" + (select ? select.value : ""));
  }
  return parts.join(",");
}

// Two classes on one day clash if each starts before the other ends
function overlaps(a, b) {
  return a.course !== b.course && a.start < b.end && b.start < a.end;
}

function shortTime(t) {
  return t.substring(0, 5);   // "10:30:00" -> "10:30"
}

// Step 2: save the courses, ask the server for this student's timetable and draw it
async function buildTimetable(save = true) {
  const message = document.getElementById("message");
  message.textContent = "";

  const reg = buildRegistration();
  if (reg === "") {
    message.textContent = "Tick at least one course.";
    return;
  }

  let data;
  try {
    if (save) await post("/api/registration", { username: student.username, reg });
    data = await get("/api/timetable?reg=" + encodeURIComponent(reg));
  } catch (error) {
    message.textContent = error.message === "Failed to fetch"
      ? "Cannot reach the ClassCue server." : error.message;
    return;
  }

  lastData = data;
  lastReg = reg;
  lastBlocks = [];          // a new registration means the old study plan no longer fits

  const clashList = document.getElementById("clashes");
  clashList.innerHTML = "";
  for (const text of data.clashes) {
    const li = document.createElement("li");
    li.textContent = text.replace(/(\d\d:\d\d):00/g, "$1");   // show 10:30, not 10:30:00
    clashList.appendChild(li);
  }

  drawFocusChoices(reg);
  document.getElementById("result").hidden = false;
  document.getElementById("prefs").hidden = false;
  document.getElementById("planMessage").textContent = "";
  drawWeek();
}

// The "hardest courses" ticks only list the courses the student registered for
function drawFocusChoices(reg) {
  const box = document.getElementById("focus");
  box.innerHTML = "";
  for (const part of reg.split(",")) {
    const code = part.split(":")[0];
    const ticked = savedFocus.includes(code) ? " checked" : "";
    box.innerHTML += `<label class="pill"><input type="checkbox" value="${esc(code)}"${ticked}> ${esc(code)}</label>`;
  }
}

// Fill the study form with what the student saved last time
function applyPreferences(prefs) {
  savedFocus = [];
  if (!prefs) {                                  // nothing saved: back to the defaults
    document.getElementById("hours").value = 8;
    document.getElementById("when").value = "17-22";
    for (const box of document.querySelectorAll("#freeDays input")) box.checked = false;
    return;
  }
  document.getElementById("hours").value = prefs.hours;
  document.getElementById("when").value = prefs.when;
  const free = prefs.free ? prefs.free.split(",") : [];
  for (const box of document.querySelectorAll("#freeDays input")) {
    box.checked = free.includes(box.value);
  }
  savedFocus = prefs.focus ? prefs.focus.split(",") : [];
}

// Draw the summary chips and the Monday-Friday grid (classes + study blocks)
function drawWeek() {
  const data = lastData;

  const clashChip = data.clashes.length === 0
    ? '<span class="chip good">No clashes</span>'
    : `<span class="chip bad">${data.clashes.length} clash${data.clashes.length === 1 ? "" : "es"}</span>`;
  const studyChip = lastBlocks.length > 0
    ? `<span class="chip">${lastBlocks.length} study hour${lastBlocks.length === 1 ? "" : "s"}</span>`
    : "";
  document.getElementById("summary").innerHTML =
    `<span class="chip">${data.slots.length} classes this week</span>` + clashChip + studyChip;

  const week = document.getElementById("week");
  week.innerHTML = "";
  for (const day of DAYS) {
    const daySlots = data.slots.filter(s => s.day === day);

    // classes and study blocks in one list, sorted by start time
    const items = daySlots.map(s => ({ kind: "class", start: shortTime(s.start), slot: s }));
    for (const b of lastBlocks) {
      if (b.day === day) items.push({ kind: "study", start: b.start, block: b });
    }
    items.sort((x, y) => x.start.localeCompare(y.start));

    const column = document.createElement("div");
    column.className = "day";
    column.innerHTML = `<h3>${day}</h3>`;

    for (const item of items) {
      const card = document.createElement("div");
      if (item.kind === "class") {
        const s = item.slot;
        const inClash = daySlots.some(other => other !== s && overlaps(s, other));
        card.className = "slot" + (inClash ? " clash" : "");
        card.style.setProperty("--c", colourOf[s.course] || "#2b4fd0");
        const group = s.group ? " " + esc(s.group) : "";
        card.innerHTML =
          (inClash ? '<span class="tag">CLASH</span>' : "") +
          `<span class="time">${shortTime(s.start)} - ${shortTime(s.end)}</span>` +
          `<div class="name">${esc(s.course)}${group}</div>` +
          `<small>${esc(s.venue)}</small><small>${esc(s.lecturer)}</small>`;
      } else {
        const b = item.block;
        card.className = "slot study";
        card.innerHTML =
          '<span class="tag">STUDY</span>' +
          `<span class="time">${esc(b.start)} - ${esc(b.end)}</span>` +
          `<div class="name">${esc(b.course)}</div>`;
      }
      column.appendChild(card);
    }
    if (items.length === 0) {
      column.innerHTML += '<div class="free">Free day</div>';
    }
    week.appendChild(column);
  }
}

// Step 3: save the study settings, ask the server for the plan and draw it
async function buildPlan(save = true) {
  const message = document.getElementById("planMessage");
  message.textContent = "";

  const hours = document.getElementById("hours").value;
  const when = document.getElementById("when").value;
  const [from, to] = when.split("-");
  const free = [...document.querySelectorAll("#freeDays input:checked")].map(i => i.value).join(",");
  const focus = [...document.querySelectorAll("#focus input:checked")].map(i => i.value).join(",");

  let data;
  try {
    if (save) {
      try {
        await post("/api/preferences", { username: student.username, hours, when, free, focus });
      } catch (error) {
        message.textContent = "Could not save your settings: " + error.message;
      }
    }
    data = await get("/api/plan?reg=" + encodeURIComponent(lastReg)
      + "&hours=" + hours + "&from=" + from + "&to=" + to
      + "&free=" + encodeURIComponent(free) + "&focus=" + encodeURIComponent(focus));
  } catch (error) {
    message.textContent = error.message === "Failed to fetch"
      ? "Cannot reach the ClassCue server." : error.message;
    return;
  }

  lastBlocks = data.blocks;
  if (lastBlocks.length === 0) {
    message.textContent = "No free hours found in that time of day. Try another time or fewer free days.";
  } else if (lastBlocks.length < Number(hours)) {
    message.textContent = "Only " + lastBlocks.length + " of " + hours + " hours fit. Try another time of day.";
  }
  drawWeek();
}

function switchStudent() {
  student = null;
  document.getElementById("app").hidden = true;
  document.getElementById("welcome").hidden = false;
  document.getElementById("profileMessage").textContent = "";
  window.scrollTo(0, 0);
}

document.getElementById("profileForm").addEventListener("submit", onContinue);
document.getElementById("build").addEventListener("click", () => buildTimetable(true));
document.getElementById("plan").addEventListener("click", () => buildPlan(true));
document.getElementById("carryAdd").addEventListener("click", addCarryOver);
document.getElementById("switch").addEventListener("click", switchStudent);

// remember the last username in this browser, as a convenience
try {
  const last = localStorage.getItem("classcueUser");
  if (last) document.getElementById("username").value = last;
} catch (e) { /* storage may be blocked */ }
