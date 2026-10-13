window.__armo = {
  measure: function() {
    var inner = document.querySelector('.inner');
    if (!inner) return { innerH: 0, contentH: 0 };
    var contentH = 0;
    for (var i = 0; i < inner.children.length; i++) {
      var el = inner.children[i];
      if (el.dataset && el.dataset.spacer !== undefined) continue;
      if (el.hasAttribute && el.hasAttribute('data-spacer')) continue;
      contentH = Math.max(contentH, el.offsetTop + el.offsetHeight);
    }
    return { innerH: inner.clientHeight, contentH: Math.round(contentH) };
  },

  fit: function(n) {
    var slide = document.querySelector('.slide');
    if (!slide) return;
    var classNames = ['slide'];
    if (n && n > 0) {
      for (var i = 1; i <= n; i++) {
        classNames.push('fit' + i);
      }
    }
    slide.className = classNames.join(' ');
  },

  build: function(data) {
    try {
      if (typeof data === 'string') {
        data = JSON.parse(data);
      }
      
      // Update GW tag
      var tagEl = document.querySelector('.tag');
      if (tagEl) {
        tagEl.textContent = 'NOTES · ' + (data.gwLabel || 'GW');
      }

      // Update Logos
      var homeLogoEl = document.querySelector('#home-crest');
      if (homeLogoEl) {
        homeLogoEl.src = data.homeCrestUrl || '';
        homeLogoEl.alt = data.homeName || '';
      }
      var awayLogoEl = document.querySelector('#away-crest');
      if (awayLogoEl) {
        awayLogoEl.src = data.awayCrestUrl || '';
        awayLogoEl.alt = data.awayName || '';
      }

      // Update Headline
      var headlineEl = document.querySelector('.headline');
      if (headlineEl) {
        headlineEl.innerHTML = (data.homeAbbr || '') + ' <span class="versus">vs</span> ' + (data.awayAbbr || '');
      }

      // Update Team Scoreline & Colors
      var homeColorDot = document.querySelector('#home-dot');
      if (homeColorDot) homeColorDot.style.background = data.homeColor || '#034694';
      var awayColorDot = document.querySelector('#away-dot');
      if (awayColorDot) awayColorDot.style.background = data.awayColor || '#DA291C';

      var homeScoreline = document.querySelector('#home-scoreline');
      if (homeScoreline) {
        homeScoreline.textContent = data.homeName + (data.homeFormation ? ' (' + data.homeFormation + ')' : '');
      }
      var awayScoreline = document.querySelector('#away-scoreline');
      if (awayScoreline) {
        awayScoreline.textContent = data.awayName + (data.awayFormation ? ' (' + data.awayFormation + ')' : '');
      }

      // Update xG Bars
      var homeBar = document.querySelector('#home-bar');
      if (homeBar) {
        homeBar.style.background = data.homeColor || '#034694';
        homeBar.style.width = data.homeBarWidthPct + '%';
        homeBar.style.flex = '0 0 ' + data.homeBarWidthPct + '%';
        homeBar.textContent = 'xG ' + (data.homeName || '').toUpperCase();
      }
      var homeXgVal = document.querySelector('#home-xg-val');
      if (homeXgVal) homeXgVal.textContent = data.homeXgStr || '0.00';

      var awayBar = document.querySelector('#away-bar');
      if (awayBar) {
        awayBar.style.background = data.awayColor || '#DA291C';
        awayBar.style.width = data.awayBarWidthPct + '%';
        awayBar.style.flex = '0 0 ' + data.awayBarWidthPct + '%';
        awayBar.textContent = 'xG ' + (data.awayName || '').toUpperCase();
      }
      var awayXgVal = document.querySelector('#away-xg-val');
      if (awayXgVal) awayXgVal.textContent = data.awayXgStr || '0.00';

      // Update Game Notes
      var gameNotesList = document.querySelector('#game-notes');
      if (gameNotesList) {
        gameNotesList.innerHTML = '';
        if (data.gameNotes && data.gameNotes.length > 0) {
          for (var gn = 0; gn < data.gameNotes.length; gn++) {
            var gnLi = document.createElement('li');
            gnLi.textContent = data.gameNotes[gn];
            gameNotesList.appendChild(gnLi);
          }
        }
      }

      // Update Paired Player Rows
      var rowsContainer = document.querySelector('.rows.two');
      if (rowsContainer) {
        rowsContainer.innerHTML = '';
        if (data.pairedRows && data.pairedRows.length > 0) {
          for (var r = 0; r < data.pairedRows.length; r++) {
            var rowData = data.pairedRows[r];
            var rDiv = document.createElement('div');
            rDiv.className = 'r';

            // Left block
            var leftCol = document.createElement('div');
            if (rowData.left && rowData.left.name) {
              var leftName = document.createElement('div');
              leftName.className = 'cellname';
              leftName.textContent = rowData.left.name;
              leftCol.appendChild(leftName);

              var leftBullets = document.createElement('ul');
              leftBullets.className = 'bullets';
              if (rowData.left.notes) {
                for (var ln = 0; ln < rowData.left.notes.length; ln++) {
                  var liL = document.createElement('li');
                  liL.textContent = rowData.left.notes[ln];
                  leftBullets.appendChild(liL);
                }
              }
              leftCol.appendChild(leftBullets);
            }
            rDiv.appendChild(leftCol);

            // Right block
            var rightCol = document.createElement('div');
            if (rowData.right && rowData.right.name) {
              var rightName = document.createElement('div');
              rightName.className = 'cellname';
              rightName.textContent = rowData.right.name;
              rightCol.appendChild(rightName);

              var rightBullets = document.createElement('ul');
              rightBullets.className = 'bullets';
              if (rowData.right.notes) {
                for (var rn = 0; rn < rowData.right.notes.length; rn++) {
                  var liR = document.createElement('li');
                  liR.textContent = rowData.right.notes[rn];
                  rightBullets.appendChild(liR);
                }
              }
              rightCol.appendChild(rightBullets);
            }
            rDiv.appendChild(rightCol);

            rowsContainer.appendChild(rDiv);
          }
        }
      }

      // Update Takeaways
      var takeawaysList = document.querySelector('#takeaways');
      if (takeawaysList) {
        takeawaysList.innerHTML = '';
        if (data.takeaways && data.takeaways.length > 0) {
          for (var t = 0; t < data.takeaways.length; t++) {
            var tLi = document.createElement('li');
            tLi.textContent = data.takeaways[t];
            takeawaysList.appendChild(tLi);
          }
        }
      }

      // Reset fit ladder to level 0
      window.__armo.fit(0);

      // Signal ready once fonts and images are ready
      Promise.all([
        document.fonts ? document.fonts.ready : Promise.resolve(),
        Promise.all(Array.prototype.slice.call(document.images).map(function(img) {
          if (img.complete) return Promise.resolve();
          if (img.decode) {
            return img.decode().catch(function() { return Promise.resolve(); });
          }
          return new Promise(function(resolve) {
            img.onload = img.onerror = resolve;
          });
        }))
      ]).then(function() {
        if (window.PosterBridge && window.PosterBridge.onReady) {
          window.PosterBridge.onReady();
        }
      }).catch(function(err) {
        if (window.PosterBridge && window.PosterBridge.onError) {
          window.PosterBridge.onError(err ? err.toString() : 'Unknown ready error');
        } else if (window.PosterBridge && window.PosterBridge.onReady) {
          window.PosterBridge.onReady();
        }
      });

    } catch (e) {
      if (window.PosterBridge && window.PosterBridge.onError) {
        window.PosterBridge.onError(e.toString());
      }
    }
  }
};
