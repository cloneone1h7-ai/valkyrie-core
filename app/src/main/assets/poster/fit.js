window.__armo = {
  appendMultilineText: function(targetEl, text) {
    if (!text) return;
    var parts = String(text).split('\n');
    for (var i = 0; i < parts.length; i++) {
      if (i > 0) targetEl.appendChild(document.createElement('br'));
      targetEl.appendChild(document.createTextNode(parts[i]));
    }
  },

  applyViewportScale: function(forceW, forceH) {
    var slide = document.querySelector('.slide');
    if (!slide) return { w: 1200, h: 1200, scale: 1 };
    var w = forceW || window.innerWidth || document.documentElement.clientWidth || 1200;
    var h = forceH || window.innerHeight || document.documentElement.clientHeight || 1200;
    if (w <= 0) w = 1200;
    if (h <= 0) h = 1200;
    var scale = Math.min(w / 1200, h / 1200);
    var x = Math.max(0, (w - 1200 * scale) / 2);
    var y = Math.max(0, (h - 1200 * scale) / 2);
    slide.style.transform = 'translate(' + x + 'px, ' + y + 'px) scale(' + scale + ')';
    console.log('[ARMO_JS] applyViewportScale: w=' + w + ', h=' + h + ', scale=' + scale + ', x=' + x + ', y=' + y);
    return { w: w, h: h, scale: scale };
  },

  measure: function() {
    var inner = document.querySelector('.inner');
    if (!inner) return { innerH: 0, contentH: 0, unscaledH: 0, scale: 1.0, fits: true };
    var unscaledH = 0;
    for (var i = 0; i < inner.children.length; i++) {
      var el = inner.children[i];
      if (el.dataset && el.dataset.spacer !== undefined) continue;
      if (el.hasAttribute && el.hasAttribute('data-spacer')) continue;
      unscaledH = Math.max(unscaledH, el.offsetTop + el.offsetHeight);
    }
    var scale = inner._scaleRatio || 1.0;
    var contentH = Math.round(unscaledH * scale);
    var innerH = inner.clientHeight;
    return {
      innerH: innerH,
      contentH: contentH,
      unscaledH: Math.round(unscaledH),
      scale: scale,
      fits: contentH <= innerH
    };
  },

  _customClasses: [],

  fit: function(n) {
    var slide = document.querySelector('.slide');
    if (!slide) return;
    var inner = document.querySelector('.inner');
    if (inner) {
      inner._scaleRatio = 1.0;
      inner.style.transform = '';
    }
    var classNames = ['slide'];
    if (this._customClasses && this._customClasses.length > 0) {
      classNames = classNames.concat(this._customClasses);
    }
    if (n && n > 0) {
      for (var i = 1; i <= n; i++) {
        classNames.push('fit' + i);
      }
    }
    slide.className = classNames.join(' ');
  },

  autoScaleToFit: function() {
    var inner = document.querySelector('.inner');
    if (!inner) return 1.0;
    
    // Reset transform to measure raw unscaled height
    inner._scaleRatio = 1.0;
    inner.style.transform = '';
    
    var m = this.measure();
    if (m.unscaledH <= m.innerH) {
      return 1.0;
    }
    
    // Calculate exact scale factor needed so content fits inside innerH
    var scale = (m.innerH - 2) / m.unscaledH;
    if (scale < 0.70) scale = 0.70; // Safety floor
    
    inner._scaleRatio = scale;
    inner.style.transformOrigin = 'top center';
    inner.style.transform = 'scale(' + scale + ')';
    console.log('[ARMO_JS] autoScaleToFit applied scale: ' + scale + ' (unscaledH=' + m.unscaledH + ', innerH=' + m.innerH + ')');
    return scale;
  },

  autoFit: function() {
    var inner = document.querySelector('.inner');
    if (inner) {
      inner._scaleRatio = 1.0;
      inner.style.transform = '';
    }
    var level = 0;
    this.fit(0);
    var m = this.measure();
    
    while (level < 10 && !m.fits) {
      level++;
      this.fit(level);
      m = this.measure();
    }
    
    if (!m.fits) {
      this.autoScaleToFit();
      m = this.measure();
    }
    
    return { level: level, fits: m.fits, contentH: m.contentH, innerH: m.innerH };
  },

  build: function(data) {
    try {
      if (typeof data === 'string') {
        data = JSON.parse(data);
      }

      // Dynamic CSS variables from studio customization
      var slideEl = document.querySelector('.slide');
      if (slideEl) {
        if (data.bgColor) slideEl.style.setProperty('--bg', data.bgColor);
        if (data.panelColor) slideEl.style.setProperty('--panel', data.panelColor);
        if (data.accentColor) slideEl.style.setProperty('--volt', data.accentColor);
        if (data.bodyTextColor) slideEl.style.setProperty('--body', data.bodyTextColor);
        if (data.bodyFontSize) slideEl.style.setProperty('--body-font-size', data.bodyFontSize + 'px');
        if (data.nameFontSize) slideEl.style.setProperty('--name-font-size', data.nameFontSize + 'px');
        if (data.fontWeight) slideEl.style.setProperty('--body-font-weight', data.fontWeight);
        if (data.lineHeight) slideEl.style.setProperty('--body-line-height', data.lineHeight);
        if (data.fontFamily) slideEl.style.setProperty('--font-body', "'" + data.fontFamily + "', sans-serif");
      }

      // Configure Customization Classes
      var customClasses = [];
      if (data.fontSizeScale === 'large') customClasses.push('font-large');
      else if (data.fontSizeScale === 'xlarge') customClasses.push('font-xlarge');
      if (data.highContrast === true) customClasses.push('high-contrast');
      if (data.canvasSpace === 'expanded') customClasses.push('canvas-expanded');
      this._customClasses = customClasses;
      
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

      // Update xG Bars (Center-Out vs Stacked vs Side-by-Side)
      var layout = data.xgLayout || 'center_out';
      var centeroutContainer = document.querySelector('#xg-container-centerout');
      var stackedContainer = document.querySelector('#xg-container-stacked');
      var sideContainer = document.querySelector('#xg-container-sidebyside');

      if (centeroutContainer) centeroutContainer.style.display = (layout === 'center_out') ? 'flex' : 'none';
      if (stackedContainer) stackedContainer.style.display = (layout === 'stacked') ? 'block' : 'none';
      if (sideContainer) sideContainer.style.display = (layout === 'side_by_side') ? 'grid' : 'none';

      // 1. Center-Out Symmetric Elements
      var homeBarCo = document.querySelector('#home-bar-co');
      if (homeBarCo) {
        homeBarCo.style.background = 'linear-gradient(270deg, ' + (data.homeColor || '#034694') + ', rgba(255,255,255,0.15))';
        homeBarCo.style.width = data.homeBarWidthPct + '%';
      }
      var homeValCo = document.querySelector('#home-xg-val-co');
      if (homeValCo) homeValCo.textContent = data.homeXgStr || '0.00';

      var awayBarCo = document.querySelector('#away-bar-co');
      if (awayBarCo) {
        awayBarCo.style.background = 'linear-gradient(90deg, ' + (data.awayColor || '#DA291C') + ', rgba(255,255,255,0.15))';
        awayBarCo.style.width = data.awayBarWidthPct + '%';
      }
      var awayValCo = document.querySelector('#away-xg-val-co');
      if (awayValCo) awayValCo.textContent = data.awayXgStr || '0.00';

      var homeNameUpper = (data.homeName || '').toUpperCase();
      var awayNameUpper = (data.awayName || '').toUpperCase();

      // 2. Stacked elements
      var homeBarStacked = document.querySelector('#home-bar-stacked');
      if (homeBarStacked) {
        homeBarStacked.style.background = data.homeColor || '#034694';
        homeBarStacked.style.width = data.homeBarWidthPct + '%';
        homeBarStacked.style.flex = '0 0 ' + data.homeBarWidthPct + '%';
        homeBarStacked.textContent = 'xG ' + homeNameUpper;
      }
      var homeValStacked = document.querySelector('#home-xg-val-stacked');
      if (homeValStacked) homeValStacked.textContent = data.homeXgStr || '0.00';

      var awayBarStacked = document.querySelector('#away-bar-stacked');
      if (awayBarStacked) {
        awayBarStacked.style.background = data.awayColor || '#DA291C';
        awayBarStacked.style.width = data.awayBarWidthPct + '%';
        awayBarStacked.style.flex = '0 0 ' + data.awayBarWidthPct + '%';
        awayBarStacked.textContent = 'xG ' + awayNameUpper;
      }
      var awayValStacked = document.querySelector('#away-xg-val-stacked');
      if (awayValStacked) awayValStacked.textContent = data.awayXgStr || '0.00';

      // 3. Side-by-side elements
      var homeBarSide = document.querySelector('#home-bar-side');
      if (homeBarSide) {
        homeBarSide.style.background = data.homeColor || '#034694';
        homeBarSide.style.width = data.homeBarWidthPct + '%';
        homeBarSide.style.flex = '0 0 ' + data.homeBarWidthPct + '%';
        homeBarSide.textContent = 'xG ' + homeNameUpper;
      }
      var homeValSide = document.querySelector('#home-xg-val-side');
      if (homeValSide) homeValSide.textContent = data.homeXgStr || '0.00';

      var awayBarSide = document.querySelector('#away-bar-side');
      if (awayBarSide) {
        awayBarSide.style.background = data.awayColor || '#DA291C';
        awayBarSide.style.width = data.awayBarWidthPct + '%';
        awayBarSide.style.flex = '0 0 ' + data.awayBarWidthPct + '%';
        awayBarSide.textContent = 'xG ' + awayNameUpper;
      }
      var awayValSide = document.querySelector('#away-xg-val-side');
      if (awayValSide) awayValSide.textContent = data.awayXgStr || '0.00';

      // Update Game Notes
      var gameNotesList = document.querySelector('#game-notes');
      if (gameNotesList) {
        gameNotesList.innerHTML = '';
        if (data.gameNotes && data.gameNotes.length > 0) {
          for (var gn = 0; gn < data.gameNotes.length; gn++) {
            var rawGn = data.gameNotes[gn];
            if (!rawGn || !rawGn.trim()) continue;
            var gnLi = document.createElement('li');
            window.__armo.appendMultilineText(gnLi, rawGn);
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
                  var rawNoteL = rowData.left.notes[ln];
                  if (!rawNoteL || !rawNoteL.trim()) continue;
                  var liL = document.createElement('li');
                  window.__armo.appendMultilineText(liL, rawNoteL);
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
                  var rawNoteR = rowData.right.notes[rn];
                  if (!rawNoteR || !rawNoteR.trim()) continue;
                  var liR = document.createElement('li');
                  window.__armo.appendMultilineText(liR, rawNoteR);
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
            var rawT = data.takeaways[t];
            if (!rawT || !rawT.trim()) continue;
            var tLi = document.createElement('li');
            window.__armo.appendMultilineText(tLi, rawT);
            takeawaysList.appendChild(tLi);
          }
        }
      }

      // Auto-fit content dynamically so live preview and renderer adapt
      window.__armo.autoFit();

      // Apply responsive viewport scale
      window.__armo.applyViewportScale();

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

window.addEventListener('resize', function() {
  if (window.__armo && window.__armo.applyViewportScale) {
    window.__armo.applyViewportScale();
  }
});

window.addEventListener('DOMContentLoaded', function() {
  if (window.__armo && window.__armo.applyViewportScale) {
    window.__armo.applyViewportScale();
  }
});

