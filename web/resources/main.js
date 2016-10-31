function toggle_visibility(id) {
   var e = document.getElementById(id);
   if(e.style.display == 'block')
      e.style.display = 'none';
   else
      e.style.display = 'block';
}

function showHide(labelid, sectionid) {
   var label = document.getElementById(labelid);
   var section = document.getElementById(sectionid);

   if(section.style.display == 'block') {
      section.style.display = 'none';
      label.innerHTML = "Show";
   } else {
      section.style.display = 'block';
      label.innerHTML = "Hide";
   }
}


function showHideSection(label, sectionid) {

   var section = document.getElementById(sectionid);

   if(label.className=='collapsable-link-opened') {
      label.className='collapsable-link-closed';
      section.className='collapsable-section-closed';
   } else {
      label.className='collapsable-link-opened';
      section.className='collapsable-section-opened';
   }
}