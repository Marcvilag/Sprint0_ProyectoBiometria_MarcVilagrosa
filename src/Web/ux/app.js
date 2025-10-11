'use strict';

const base = window.location.origin;
const $ = (id) => document.getElementById(id);

const saveMsg   = $("saveMsg");
const saveOut   = $("saveOut");
const ultimaOut = $("ultimaOut");
const healthMsg = $("healthMsg");
const autoMsg   = $("autoMsg");

let autoTimer = null;

async function guardarMedicion(valor){
  saveMsg.textContent = "Guardando…";
  saveOut.style.display = "none";
  try{
    const r = await fetch(`${base}/api/v1/mediciones`, {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({valor: Number(valor)})
    });
    if(!r.ok){
      const t = await r.text();
      throw new Error(`HTTP ${r.status} – ${t}`);
    }
    const data = await r.json();
    saveMsg.innerHTML = `<span class="ok">✓ Guardado</span>`;
    saveOut.textContent = JSON.stringify(data, null, 2);
    saveOut.style.display = "block";
  }catch(err){
    saveMsg.innerHTML = `<span class="err">✗ Error: ${err.message}</span>`;
  }
}

async function cargarUltima(){
  ultimaOut.style.display = "none";
  try{
    const r = await fetch(`${base}/api/v1/mediciones/ultima`);
    if(!r.ok){
      const t = await r.text();
      throw new Error(`HTTP ${r.status} – ${t}`);
    }
    const data = await r.json();
    ultimaOut.textContent = JSON.stringify(data, null, 2);
    ultimaOut.style.display = "block";
  }catch(err){
    ultimaOut.textContent = `Error: ${err.message}`;
    ultimaOut.style.display = "block";
  }
}

async function health(){
  healthMsg.textContent = "Consultando…";
  try{
    const r = await fetch(`${base}/api/v1/health`);
    const data = await r.json();
    healthMsg.textContent = JSON.stringify(data);
  }catch(err){
    healthMsg.textContent = `Error: ${err.message}`;
  }
}

$("btnGuardar").addEventListener("click", ()=>{
  const v = $("valor").value;
  if(v === "" || isNaN(Number(v))){
    saveMsg.innerHTML = '<span class="err">Introduce un número.</span>';
    return;
  }
  guardarMedicion(v);
});

$("btnDemo").addEventListener("click", ()=>{
  const demo = (Math.random()*10+20).toFixed(2);
  $("valor").value = demo;
  guardarMedicion(demo);
});

$("btnUltima").addEventListener("click", cargarUltima);

$("btnAuto").addEventListener("click", ()=>{
  if(autoTimer){
    clearInterval(autoTimer);
    autoTimer = null;
    autoMsg.textContent = "Auto-refresco desactivado.";
  }else{
    cargarUltima();
    autoTimer = setInterval(cargarUltima, 2000);
    autoMsg.textContent = "Auto-refresco cada 2 segundos.";
  }
});

$("btnHealth").addEventListener("click", health);

// Comprobación inicial
health();
