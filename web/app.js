// Address of the Java server
const API = "http://localhost:8080";
const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];

// One jewel colour per course, so the week is easy to scan
const COLOURS = ["#0f8a63", "#2b4fd0", "#7b4bb7", "#d98a0b", "#0e8fa0", "#6b9b1f", "#4b3fa8", "#d0508f"];
const colourOf = {};

let courses = [];
let lastData = null;      // the student's classes from the server
let lastBlocks = [];      // the study blocks from the server
let lastReg = "";         // the registration the timetable was built from

// Step 1: ask the server for the course list and draw it
async function loadCourses() {
  const box = document.getElementById("courses");
  try {
    const response = await fetch(API + "/api/courses");
    courses = await response.json();
  } catch (error) {
    box.textContent = "Cannot reach the ClassCue server. Is Server.java running?";
    return;
  }

  box.innerHTML = "";
  courses.forEach((c, i) => {
    colourOf[c.code] = COLOURS[i % COLOURS.length];
    const row = document.createElement("div");
    row.className = "course";
    row.style.setProperty("--c", colourOf[c.code]);

    let groupPicker = "";
    if (c.groups.length > 0) {
      const options = c.groups.map(g => `<option value="${g}">Group ${g}</option>`).join("");
      groupPicker = `<select id="group-${c.code}">${options}</select>`;
    }

    row.innerHTML =
      `<input type="checkbox" id="check-${c.code}" checked>` +
      `<label for="check-${c.code}"><b>${c.code}</b> ${c.title} <small>(${c.credits} credit${c.credits === 1 ? "" : "s"})</small></label>` +
      groupPicker;
    box.appendChild(row);
  });
}

// Turn the ticked courses into "CE 251:,CE 271:A,..."
function buildRegistration() {
  const parts = [];
  for (const c of courses) {
    if (!document.getElementById("check-" + c.code).checked) continue;
    const select = document.getElementById("group-" + c.code);
    parts.push(c.code + ":" + (select ? select.value : ""));
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

// Step 2: ask the server for this student's timetable and draw it
async function buildTimetable() {
  const message = document.getElementById("message");
  message.textContent = "";

  const reg = buildRegistration();
  if (reg === "") {
    message.textContent = "Tick at least one course.";
    return;
  }

  let data;
  try {
    const response = await fetch(API + "/api/timetable?reg=" + encodeURIComponent(reg));
    data = await response.json();
  } catch (error) {
    message.textContent = "Cannot reach the ClassCue server.";
    return;
  }
  if (data.error) {
    message.textContent = "Server error: " + data.error;
    return;
  }

  lastData = data;
  lastReg = reg;
  lastBlocks = [];          // a new registration means the old study plan no longer fits

  const clashList = document.getElementById("clashes");
  clashList.innerHTML = "";
  for (const text of data.clashes) {
    const li = document.createElement("li");
    li.textContent = text;
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
    box.innerHTML += `<label class="pill"><input type="checkbox" value="${code}"> ${code}</label>`;
  }
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
        const group = s.group ? " " + s.group : "";
        card.innerHTML =
          (inClash ? '<span class="tag">CLASH</span>' : "") +
          `<span class="time">${shortTime(s.start)} - ${shortTime(s.end)}</span>` +
          `<div class="name">${s.course}${group}</div>` +
          `<small>${s.venue}</small><small>${s.lecturer}</small>`;
      } else {
        const b = item.block;
        card.className = "slot study";
        card.innerHTML =
          '<span class="tag">STUDY</span>' +
          `<span class="time">${b.start} - ${b.end}</span>` +
          `<div class="name">${b.course}</div>`;
      }
      column.appendChild(card);
    }
    if (items.length === 0) {
      column.innerHTML += '<div class="free">Free day</div>';
    }
    week.appendChild(column);
  }
}

// Step 3: send the study preferences to the server and draw the plan
async function buildPlan() {
  const message = document.getElementById("planMessage");
  message.textContent = "";

  const hours = document.getElementById("hours").value;
  const [from, to] = document.getElementById("when").value.split("-");
  const free = [...document.querySelectorAll("#freeDays input:checked")].map(i => i.value);
  const focus = [...document.querySelectorAll("#focus input:checked")].map(i => i.value);

  const url = API + "/api/plan?reg=" + encodeURIComponent(lastReg)
    + "&hours=" + hours + "&from=" + from + "&to=" + to
    + "&free=" + encodeURIComponent(free.join(","))
    + "&focus=" + encodeURIComponent(focus.join(","));

  let data;
  try {
    const response = await fetch(url);
    data = await response.json();
  } catch (error) {
    message.textContent = "Cannot reach the ClassCue server.";
    return;
  }
  if (data.error) {
    message.textContent = "Server error: " + data.error;
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

document.getElementById("build").addEventListener("click", buildTimetable);
document.getElementById("plan").addEventListener("click", buildPlan);
loadCourses();
