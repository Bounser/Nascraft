const dateFns = Chart._adapters._date.fns;

document.addEventListener('DOMContentLoaded', () => {
    const itemListElement = document.getElementById('item-list');
    const searchInputElement = document.getElementById('search-input');
    const selectedItemIconElement = document.getElementById('selected-item-icon');
    const selectedItemNameElement = document.getElementById('selected-item-name');
    const selectedItemDescElement = document.getElementById('selected-item-description');
    const currentPriceElement = document.getElementById('current-price');
    const sellPriceDisplayElement = document.getElementById('sell-price-display');
    const buyPriceDisplayElement = document.getElementById('buy-price-display');
    const itemPriceChartContainer = document.getElementById('item-price-chart-container');
    const cpiChartCanvas = document.getElementById('cpi-chart');
    const marketCapTreemapContainer = document.getElementById('treemap-container');
    const inflationCheckbox = document.getElementById('inflation-adjust-checkbox');
    const logScaleCheckbox = document.getElementById('log-scale-checkbox');
    const marketRankElement = document.getElementById('market-rank');
    const allTimeHighElement = document.getElementById('all-time-high');
    const allTimeLowElement = document.getElementById('all-time-low');
    const resetButton = document.getElementById('reset-zoom-button');
    const chartSpinner = document.getElementById('chart-loading-spinner');
    const change1hElement = document.getElementById('change-1h');
    const inceptionReturnElement = document.getElementById('inception-return');
    const sortControlsContainer = document.getElementById('sort-controls');
    const treemapTooltip = document.getElementById('treemap-tooltip');
    const volatilityElement = document.getElementById('volatility');
    const maxDrawdownElement = document.getElementById('max-drawdown');
    const topPortfoliosContainer = document.getElementById('top-portfolios-container');


    let lightweightChart = null;
    let mainPriceSeries = null;
    let cpiChart = null;

    let allItems = [];
    let previousPrices = new Map();
    let selectedItemIdentifier = null;
    let cpiDataStore = [];
    let currentItemNominalData = [];
    let priceSortState = 0;
    let changeSortState = 0;
    let operationsSortState = 0;
    let pollingIntervalId = null;


    const API_BASE_URL = '/api';
    const POLLING_INTERVAL = 3000;

    const defaultChartJsOptions = {
        maintainAspectRatio: false, responsive: true,
        animation: { duration: 800, easing: 'easeInOutQuad', },
        plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false, backgroundColor: 'rgba(31, 41, 55, 0.9)', titleColor: '#f3f4f6', bodyColor: '#d1d5db', borderColor: '#4b5563', borderWidth: 1, padding: 10, cornerRadius: 4, callbacks: { title: function(tooltipItems) { if (tooltipItems.length > 0) { const item = tooltipItems[0]; try { const date = new Date(item.parsed.x); return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }); } catch (e) { console.error("Error formatting tooltip title with native Date:", e); return ''; } } return ''; } } } },
        scales: { x: { grid: { color: 'rgba(75, 85, 99, 0.2)' }, ticks: { color: '#9ca3af', maxRotation: 0, autoSkip: true, } }, y: { grid: { color: 'rgba(75, 85, 99, 0.2)' }, ticks: { color: '#9ca3af' } } },
        layout: { padding: 5 },
    };

    const lightweightChartOptions = {
        layout: { background: { color: 'transparent' }, textColor: '#D1D5DB', },
        grid: { vertLines: { color: 'rgba(75, 85, 99, 0.3)' }, horzLines: { color: 'rgba(75, 85, 99, 0.3)' }, },
        crosshair: { mode: LightweightCharts.CrosshairMode.Normal, },
        rightPriceScale: { borderColor: 'rgba(192, 192, 192, 0.3)', mode: LightweightCharts.PriceScaleMode.Normal, autoScale: true },
        timeScale: { borderColor: 'rgba(192, 192, 192, 0.3)', timeVisible: true, secondsVisible: false, },
        handleScroll: true, handleScale: true,
    };
    const mainPriceSeriesOptions = {
        topLineColor: 'rgba(52, 211, 153, 1)',
        topFillColor1: 'rgba(52, 211, 153, 0.2)',
        topFillColor2: 'rgba(52, 211, 153, 0.02)',
        bottomLineColor: 'rgba(248, 113, 113, 1)',
        bottomFillColor1: 'rgba(248, 113, 113, 0.02)',
        bottomFillColor2: 'rgba(248, 113, 113, 0.2)',
        lineWidth: 2,
        priceFormat: { type: 'price', precision: 2, minMove: 0.01, },
    };


    function formatCurrency(value, defaultVal = '-') {
        if (value === null || value === undefined || isNaN(value)) {
            return defaultVal;
        }
        return '$' + value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function calculateVolatility(prices) {
        const n = prices.length;
        if (n < 2) return 0;
        const mean = prices.reduce((a, b) => a + b, 0) / n;
        if (Math.abs(mean) < 1e-9) return 0;
        const variance = prices.reduce((sq, val) => sq + Math.pow(val - mean, 2), 0) / n;
        const stdDev = Math.sqrt(variance);
        return stdDev / mean;
    }

    function calculateMaxDrawdown(prices) {
        if (!prices || prices.length < 2) return 0;
        let maxDrawdown = 0;
        let peak = prices[0];
        for (let i = 1; i < prices.length; i++) {
            if (prices[i] > peak) {
                peak = prices[i];
            } else if (peak > 0) {
                const drawdown = (peak - prices[i]) / peak;
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }
        return -maxDrawdown;
    }

    function calculateInceptionReturn(prices) {
        if (!prices || prices.length < 2) return null;
        const firstPrice = prices[0];
        const lastPrice = prices[prices.length - 1];
        if (firstPrice === undefined || firstPrice === 0 || lastPrice === undefined) return null;
        return (lastPrice - firstPrice) / firstPrice;
    }


    async function fetchData(endpoint) {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`);
            if (!response.ok) { let errorBody = ''; try { errorBody = await response.text(); } catch (e) {} console.error(`HTTP error! Status: ${response.status} on ${endpoint}. Body: ${errorBody}`); throw new Error(`HTTP error! Status: ${response.status} on ${endpoint}`); }
            if (response.status === 204) { return null; }
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) { return await response.json(); }
            else { console.warn(`Received non-JSON response from ${endpoint}. Content-Type: ${contentType}`); return await response.text(); }
        } catch (error) { console.error(`Failed to fetch ${endpoint}:`, error); throw error; }
     }

     function applyFlashAnimation(element, direction) {
        if (!element) return;
        const animationClass = direction === 'up' ? 'flash-up' : 'flash-down';
        if (element.dataset.isAnimating === 'true') return;
        element.dataset.isAnimating = 'true';
        const flashElement = document.createElement('div');
        flashElement.className = `update-flash ${animationClass}`;
        element.style.position = 'relative';
        element.style.zIndex = '0';
        element.prepend(flashElement);
        setTimeout(() => {
            flashElement.remove();
            delete element.dataset.isAnimating;
        }, 700);
    }

    function renderItemList(itemsToRender, priceChangesMap = null, hottestItemId = null) {
        if (!itemListElement) { console.error("Item list element not found!"); return; }
        const fragment = document.createDocumentFragment();
        let hasContent = false;

        if (itemsToRender && itemsToRender.length > 0) {
            hasContent = true;
            itemsToRender.forEach(item => {
                const itemIdentifier = item?.identifier;
                const itemName = item?.name || 'Unknown Item';
                const itemPrice = item?.price;
                const itemChangePercent = item?.changePercent;

                if (itemIdentifier === null || itemIdentifier === undefined) { console.warn('Skipping item in list due to missing identifier:', item); return; }
                const itemIdentifierStr = String(itemIdentifier);
                const selectedIdentifierStr = selectedItemIdentifier !== null ? String(selectedItemIdentifier) : null;

                const li = document.createElement('li');
                li.className = `flex items-center justify-between p-2 rounded-md cursor-pointer hover:bg-gray-700 transition duration-150 ease-in-out ${itemIdentifierStr === selectedIdentifierStr ? 'item-selected' : ''}`;
                li.dataset.identifier = itemIdentifierStr;

                const priceDiv = `<div class="text-sm font-semibold text-white">${formatCurrency(itemPrice)}</div>`;
                const changeDiv = `<div class="text-xs ${getTrendClass(itemChangePercent)}">${formatTrend(itemChangePercent, false)}</div>`;

                const fireIconHTML = itemIdentifierStr === hottestItemId ? '<img src="/api/icons/fire.png" alt="Hot" class="fire-icon">' : '';

                const iconContainerHTML = `
                    <div class="flex items-center mr-2 min-w-0" data-role="icon-container">
                        <img src="${API_BASE_URL}/icons/${itemIdentifierStr}.png" alt="${itemName}" class="w-6 h-6 rounded shrink-0" onerror="this.src='https://placehold.co/24x24/374151/9ca3af?text=?'; this.onerror=null;">
                        ${fireIconHTML}
                        <span class="truncate text-sm font-medium text-gray-100 inline-flex items-center ml-1">
                            ${itemName}
                        </span>
                    </div>`;

                li.innerHTML = `
                    ${iconContainerHTML}
                    <div data-role="price-change-container" class="text-right shrink-0 ml-2 relative">
                        ${priceDiv}
                        ${changeDiv}
                    </div>`;

                if (priceChangesMap) {
                    const changeDirection = priceChangesMap.get(itemIdentifierStr);
                    if (changeDirection === 'up' || changeDirection === 'down') {
                        const priceChangeContainer = li.querySelector('[data-role="price-change-container"]');
                        if(priceChangeContainer) applyFlashAnimation(priceChangeContainer, changeDirection);
                    }
                }

                li.addEventListener('click', () => handleItemSelection(itemIdentifierStr));
                fragment.appendChild(li);
            });
        }

        itemListElement.innerHTML = '';
        if (hasContent) {
             itemListElement.appendChild(fragment);
        } else {
             const searchTerm = searchInputElement.value;
             itemListElement.innerHTML = `<li class="p-2 text-gray-400">${searchTerm ? 'No items match search.' : 'No items found.'}</li>`;
        }
    }


    async function handleItemSelection(identifier) {
        selectedItemIdentifier = identifier;
        document.querySelectorAll('#item-list li').forEach(li => { li.classList.toggle('item-selected', li.dataset.identifier === identifier); });
        const selectedItem = allItems.find(item => String(item?.identifier) === identifier);

        currentItemNominalData = [];
        let calculatedRank = null;
        let calculatedATH = null;
        let calculatedATL = null;
        let calculatedVolatility = null;
        let calculatedMaxDrawdown = null;
        let calculatedInceptionReturn = null;

        if (chartSpinner) chartSpinner.classList.remove('hidden');
        clearLightweightChart();

        if (selectedItem) {
            selectedItemIconElement.src = `${API_BASE_URL}/icons/${selectedItem.identifier}.png`;
            selectedItemIconElement.onerror = () => { selectedItemIconElement.src='https://placehold.co/32x32/374151/9ca3af?text=?'; selectedItemIconElement.onerror=null; };
            selectedItemNameElement.textContent = selectedItem.name || 'Unknown Item';
            selectedItemDescElement.textContent = `Loading price evolution for ${selectedItem.name}...`;

            const sortedByPrice = [...allItems]
                .filter(item => item && item.price !== null && item.price !== undefined && !isNaN(item.price))
                .sort((a, b) => b.price - a.price);
            const rankIndex = sortedByPrice.findIndex(item => String(item?.identifier) === identifier);
            calculatedRank = rankIndex !== -1 ? rankIndex + 1 : null;

             updateSelectedItemDetails(selectedItem, calculatedRank, null, null, null, null, null);

            try {
                const endpoint = `/charts/item/${identifier}`;
                const itemChartDataRaw = await fetchData(endpoint);

                if (Array.isArray(itemChartDataRaw) && itemChartDataRaw.length > 0) {
                    currentItemNominalData = itemChartDataRaw
                        .filter(dp => dp.time !== null && dp.time !== undefined && !isNaN(dp.time) && dp.value !== null && dp.value !== undefined && !isNaN(dp.value))
                        .sort((a, b) => a.time - b.time);

                    const prices = currentItemNominalData.map(dp => dp.value);
                    if (prices.length > 0) {
                        calculatedATH = Math.max(...prices);
                        calculatedATL = Math.min(...prices);
                        calculatedVolatility = calculateVolatility(prices);
                        calculatedMaxDrawdown = calculateMaxDrawdown(prices);
                        calculatedInceptionReturn = calculateInceptionReturn(prices);
                    }

                    updateSelectedItemDetails(selectedItem, calculatedRank, calculatedATH, calculatedATL, calculatedVolatility, calculatedMaxDrawdown, calculatedInceptionReturn);
                    displayCurrentItemChart();

                } else {
                    updateSelectedItemDetails(selectedItem, calculatedRank, null, null, null, null, null);
                    selectedItemDescElement.textContent = `No price history found for ${selectedItem.name}.`;
                    clearLightweightChart();
                }
            } catch (error) {
                console.error(`Failed to fetch or update chart for ${identifier}:`, error);
                updateSelectedItemDetails(selectedItem, calculatedRank, null, null, null, null, null);
                selectedItemDescElement.textContent = `Error loading price history for ${selectedItem.name}.`;
                clearLightweightChart();
            } finally {
                 if (chartSpinner) chartSpinner.classList.add('hidden');
            }
        } else {
            updateSelectedItemDetails(null, null, null, null, null, null, null);
            clearLightweightChart();
            if (chartSpinner) chartSpinner.classList.add('hidden');
        }
    }

    function updateSelectedItemDetails(item, rank, ath, atl, volatility, maxDrawdown, inceptionReturn) {
        if (!currentPriceElement || !marketRankElement || !allTimeHighElement || !allTimeLowElement || !sellPriceDisplayElement || !buyPriceDisplayElement || !change1hElement || !inceptionReturnElement || !volatilityElement || !maxDrawdownElement) {
            console.error("One or more item detail elements are missing!");
            return;
        }

        if (item) {
            const itemIdentifierStr = String(item?.identifier);
            const itemName = item?.name || 'Unknown Item';
            const itemChangePercent = item?.changePercent;
            const itemPrice = item?.price;

            selectedItemIconElement.src = `${API_BASE_URL}/icons/${itemIdentifierStr}.png`;
            selectedItemIconElement.onerror = () => { selectedItemIconElement.src='https://placehold.co/32x32/374151/9ca3af?text=?'; selectedItemIconElement.onerror=null; };
            selectedItemNameElement.textContent = itemName;

             if (!selectedItemDescElement.textContent?.startsWith('Loading')) {
                 selectedItemDescElement.textContent = item?.description || `Evolution of ${itemName}.`;
             } else if (ath !== null || atl !== null) {
                 selectedItemDescElement.textContent = item?.description || `Evolution of ${itemName}.`;
             }


            currentPriceElement.textContent = formatCurrency(itemPrice);
            marketRankElement.textContent = rank ? `#${rank}` : '-';
            allTimeHighElement.textContent = formatCurrency(ath, '-');
            allTimeLowElement.textContent = formatCurrency(atl, '-');
            change1hElement.textContent = formatTrend(itemChangePercent, true, '-');
            change1hElement.className = `text-lg font-semibold ${getTrendClass(itemChangePercent)}`;

            inceptionReturnElement.textContent = (inceptionReturn !== null && !isNaN(inceptionReturn))
                ? formatTrend(inceptionReturn * 100, true, '-')
                : '-';
            inceptionReturnElement.className = `text-lg font-semibold ${getTrendClass(inceptionReturn * 100)}`;


            volatilityElement.textContent = (volatility !== null && !isNaN(volatility))
                ? `${(volatility * 100).toFixed(1)}%`
                : '-';
             volatilityElement.className = 'text-base font-medium text-blue-300';

            maxDrawdownElement.textContent = (maxDrawdown !== null && !isNaN(maxDrawdown))
                ? formatTrend(maxDrawdown * 100, true, '-')
                : '-';
            maxDrawdownElement.className = `text-base font-medium ${getTrendClass(maxDrawdown * 100)}`;


            sellPriceDisplayElement.textContent = formatCurrency(item?.sell, '-');
            buyPriceDisplayElement.textContent = formatCurrency(item?.buy, '-');

        } else {
            selectedItemIconElement.src = 'https://placehold.co/32x32/374151/9ca3af?text=?';
            selectedItemNameElement.textContent = 'Select an Item';
            selectedItemDescElement.textContent = 'Select an item to see its price evolution.';
            currentPriceElement.textContent = '-';
            marketRankElement.textContent = '-';
            change1hElement.textContent = '-'; change1hElement.className = 'text-lg font-semibold';
            inceptionReturnElement.textContent = '-'; inceptionReturnElement.className = 'text-lg font-semibold';
            allTimeHighElement.textContent = '-'; allTimeLowElement.textContent = '-';
            volatilityElement.textContent = '-'; volatilityElement.className = 'text-base font-medium text-blue-300';
            maxDrawdownElement.textContent = '-'; maxDrawdownElement.className = 'text-base font-medium text-orange-400';
            sellPriceDisplayElement.textContent = '-'; buyPriceDisplayElement.textContent = '-';
        }
    }

    function formatTrend(changePercent, showSign = true, defaultVal = '-') {
        const trendNum = parseFloat(changePercent);
        if (changePercent === null || changePercent === undefined || isNaN(trendNum)) { return defaultVal; }
        const sign = showSign && trendNum > 0 ? '+' : '';
        if (!showSign && trendNum < 0) {
             return `${trendNum.toFixed(2)}%`;
        }
        return `${sign}${trendNum.toFixed(2)}%`;
     }

    function getTrendClass(changePercent) {
        const trendNum = parseFloat(changePercent);
        if (changePercent === null || changePercent === undefined || isNaN(trendNum) || Math.abs(trendNum) < 0.001) { return 'trend-neutral'; }
        else if (trendNum > 0) { return 'trend-up'; }
        else { return 'trend-down'; }
     }

    function sortAndRenderItems(priceChangesMap = null) {
         if (!Array.isArray(allItems)) {
             renderItemList([], null, null);
             return;
         }
         const searchTerm = searchInputElement.value.toLowerCase().trim();
         let itemsToDisplay = [...allItems];

         if (searchTerm) {
             itemsToDisplay = itemsToDisplay.filter(item => {
                 const itemName = item?.name ? item.name.toLowerCase() : '';
                 const itemIdentifier = item?.identifier ? String(item.identifier).toLowerCase() : '';
                 return itemName.includes(searchTerm) || itemIdentifier.includes(searchTerm);
             });
         }

         let sortCriteria = 'name';
         if (priceSortState === 1) sortCriteria = 'price_desc';
         else if (priceSortState === 2) sortCriteria = 'price_asc';
         else if (changeSortState === 1) sortCriteria = 'change_desc';
         else if (changeSortState === 2) sortCriteria = 'change_asc';
         else if (operationsSortState === 1) sortCriteria = 'operations_desc';
         else if (operationsSortState === 2) sortCriteria = 'operations_asc';

         let hottestItemId = null;
         if (allItems.length > 0) {
             let maxOps = -1;
             allItems.forEach(item => {
                 const ops = item.operations ?? 0;
                 if (ops > maxOps) {
                     maxOps = ops;
                     hottestItemId = String(item.identifier);
                 }
             });
              if (maxOps <= 0) hottestItemId = null;
         }


         itemsToDisplay.sort((a, b) => {
             const aValPrice = a?.price ?? 0;
             const bValPrice = b?.price ?? 0;
             const aValChange = a?.changePercent ?? 0;
             const bValChange = b?.changePercent ?? 0;
             const aValOps = a?.operations ?? 0;
             const bValOps = b?.operations ?? 0;
             const aName = a?.name ?? '';
             const bName = b?.name ?? '';

             switch (sortCriteria) {
                 case 'price_desc': return bValPrice - aValPrice;
                 case 'price_asc': return aValPrice - bValPrice;
                 case 'change_desc': return bValChange - aValChange;
                 case 'change_asc': return aValChange - bValChange;
                 case 'operations_desc': return bValOps - aValOps;
                 case 'operations_asc': return aValOps - bValOps;
                 case 'name': default: return aName.localeCompare(bName);
             }
         });

         renderItemList(itemsToDisplay, priceChangesMap, hottestItemId);
    }


    function handleSortClick(event) {
         const clickedButton = event.target.closest('.sort-button');
         if (!clickedButton || !sortControlsContainer.contains(clickedButton)) {
             return;
         }

         const sortCategory = clickedButton.dataset.sortCategory;

         if (sortCategory === 'price') {
             priceSortState = (priceSortState + 1) % 3;
             changeSortState = 0;
             operationsSortState = 0;
         } else if (sortCategory === 'change') {
             changeSortState = (changeSortState + 1) % 3;
             priceSortState = 0;
             operationsSortState = 0;
         } else if (sortCategory === 'operations') {
             operationsSortState = (operationsSortState + 1) % 3;
             priceSortState = 0;
             changeSortState = 0;
         } else {
             return;
         }

         sortControlsContainer.querySelectorAll('.sort-button').forEach(btn => {
             const category = btn.dataset.sortCategory;
             const indicator = btn.querySelector('.sort-indicator');
             let state = 0;
             if (category === 'price') state = priceSortState;
             else if (category === 'change') state = changeSortState;
             else if (category === 'operations') state = operationsSortState;


             btn.classList.toggle('active-sort', state > 0);
             if (indicator) {
                 if (state === 1) {
                     indicator.textContent = '▼';
                     indicator.style.display = 'inline-block';
                 } else if (state === 2) {
                     indicator.textContent = '▲';
                     indicator.style.display = 'inline-block';
                 } else {
                     indicator.textContent = '';
                     indicator.style.display = 'none';
                 }
             }
         });

         sortAndRenderItems();
    }


    function findCpiForTimestamp(timestampSec, sortedCpiData) {
        if (!sortedCpiData || sortedCpiData.length === 0) return null;
        let bestMatchIndex = -1;
        for (let i = 0; i < sortedCpiData.length; i++) { if (sortedCpiData[i].time <= timestampSec) { bestMatchIndex = i; } else { break; } }
        if (bestMatchIndex !== -1) { return sortedCpiData[bestMatchIndex].value; }
        return null;
    }

    function adjustPricesForInflation(nominalPrices, cpiData) {
        if (!cpiData || cpiData.length === 0) { return nominalPrices; }
        return nominalPrices.map(pricePoint => {
            const cpiValue = findCpiForTimestamp(pricePoint.time, cpiData);
            let adjustedValue = pricePoint.value;
            if (cpiValue !== null && cpiValue > 0) { adjustedValue = pricePoint.value / (cpiValue / 100.0); }
            return { time: pricePoint.time, value: adjustedValue };
        }).filter(dp => dp.value !== undefined && !isNaN(dp.value));
     }

     function displayCurrentItemChart() {
         if (!currentItemNominalData || currentItemNominalData.length === 0) {
             clearLightweightChart();
             return;
         }
         const adjust = inflationCheckbox?.checked ?? false;
         let dataToDisplay = adjust ? adjustPricesForInflation(currentItemNominalData, cpiDataStore) : currentItemNominalData;
         updateLightweightChart(dataToDisplay);
     }

     function handleInflationToggle() {
         displayCurrentItemChart();
     }

     function handleLogScaleToggle() {
        if (!lightweightChart) return;
        const useLogScale = logScaleCheckbox?.checked ?? false;
        lightweightChart.applyOptions({
             rightPriceScale: {
                 mode: useLogScale ? LightweightCharts.PriceScaleMode.Logarithmic : LightweightCharts.PriceScaleMode.Normal,
             },
        });
     }

     function handleResetZoom() {
         if (lightweightChart) {
             lightweightChart.timeScale().fitContent();
             lightweightChart.applyOptions({
                 rightPriceScale: { autoScale: true },
             });
             if (logScaleCheckbox?.checked) {
                 logScaleCheckbox.checked = false;
                 lightweightChart.applyOptions({
                     rightPriceScale: { mode: LightweightCharts.PriceScaleMode.Normal },
                 });
             }
         }
     }


    function initializeLightweightChart() {
        if (!itemPriceChartContainer) { console.error("Lightweight Chart container not found!"); return; }
        if (typeof LightweightCharts === 'undefined' || !LightweightCharts) { console.error("LightweightCharts library is not loaded!"); itemPriceChartContainer.innerHTML = '<div class="flex items-center justify-center h-full text-red-500 p-4">Charting library failed to load.</div>'; return; }
        try {
            lightweightChart = LightweightCharts.createChart(itemPriceChartContainer, lightweightChartOptions);
            if (lightweightChart && typeof lightweightChart.addBaselineSeries === 'function') {
                mainPriceSeries = lightweightChart.addBaselineSeries(mainPriceSeriesOptions);
            } else {
                 console.error('lightweightChart.addBaselineSeries is not available or not a function.');
                 throw new TypeError('lightweightChart.addBaselineSeries is not available or not a function.');
            }
            const resizeObserver = new ResizeObserver(entries => { if (!entries || entries.length === 0) { return; } const rect = entries[0].contentRect; if (rect.width > 0 && rect.height > 0 && lightweightChart) { lightweightChart.resize(rect.width, rect.height); } });
            resizeObserver.observe(itemPriceChartContainer);
        } catch (error) { console.error("Failed to initialize Lightweight Chart:", error); itemPriceChartContainer.innerHTML = '<div class="flex items-center justify-center h-full text-red-500 p-4">Error initializing chart. Check console.</div>'; mainPriceSeries = null; lightweightChart = null; }
    }


    function updateLightweightChart(timeSeriesData) {
        if (!mainPriceSeries) { console.error("Main price series not initialized. Cannot update chart."); return; }
        if (!Array.isArray(timeSeriesData)) { console.error("Invalid timeSeriesData for Lightweight Chart:", timeSeriesData); clearLightweightChart(); return; }

        const formattedData = timeSeriesData
            .filter(dp => dp.value !== undefined && dp.time !== null && dp.time !== undefined && !isNaN(dp.time))
            .sort((a, b) => a.time - b.time);

        if (formattedData.length === 0) {
             clearLightweightChart();
             return;
        }

        try {
            const firstValue = formattedData[0].value;
            mainPriceSeries.applyOptions({
                 baseValue: { type: 'price', price: firstValue }
            });

            mainPriceSeries.setData(formattedData);

            if (lightweightChart) {
                 lightweightChart.timeScale().fitContent();
                 lightweightChart.priceScale().applyOptions({ autoScale: true });
            }
        }
        catch (error) { console.error("Error setting data on Lightweight Chart:", error); }
     }

    function clearLightweightChart() {
        if (mainPriceSeries) { try { mainPriceSeries.setData([]); } catch (error) { console.error("Error clearing data on Lightweight Chart:", error); } }
     }

    function createCpiGradient(context) {
        const chart = context.chart; const {ctx, chartArea} = chart;
        if (!chartArea) { return 'rgba(167, 139, 250, 0.2)'; }
        const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
        gradient.addColorStop(0, 'rgba(167, 139, 250, 0.6)'); gradient.addColorStop(0.8, 'rgba(167, 139, 250, 0.1)'); gradient.addColorStop(1, 'rgba(167, 139, 250, 0)');
        return gradient;
     }


    function updateCpiChart(timeSeriesData) {
        if (!cpiChartCanvas) { console.error("CPI chart canvas not found!"); return; }
        if (!Array.isArray(timeSeriesData)) { console.error("Invalid timeSeriesData for CPI Chart:", timeSeriesData); return; }
         const formattedData = timeSeriesData
             .map(dp => ({ x: new Date(dp.time * 1000), y: dp.value !== null && dp.value !== undefined ? parseFloat(dp.value) : null }))
             .filter(dp => !isNaN(dp.x.getTime()) && dp.y !== null);
         formattedData.sort((a, b) => a.x - b.x);

        const chartData = {
            datasets: [{
                label: 'CPI', data: formattedData, borderColor: '#a78bfa', backgroundColor: createCpiGradient,
                fill: true, borderWidth: 1.5, pointRadius: 0, tension: 0
            }]
        };

        if (cpiChart) { cpiChart.data = chartData; cpiChart.update(); }
        else {
            const config = { type: 'line', data: chartData,
                options: {
                    ...defaultChartJsOptions,
                    interaction: { mode: 'index', intersect: false },
                    plugins: {
                         ...defaultChartJsOptions.plugins,
                         tooltip: {
                              ...defaultChartJsOptions.plugins.tooltip,
                              callbacks: {
                                   ...defaultChartJsOptions.plugins.tooltip.callbacks,
                                   label: function(context) {
                                        let label = context.dataset.label || '';
                                        if (label) { label += ': '; }
                                        if (context.parsed.y !== null) { label += context.parsed.y.toFixed(1); }
                                        return label;
                                   }
                              }
                         }
                    },
                    scales: {
                        x: {
                            ...defaultChartJsOptions.scales.x, type: 'time',
                            time: { unit: 'day',
                                displayFormats: { millisecond: 'HH:mm:ss.SSS', second: 'HH:mm:ss', minute: 'HH:mm', hour: 'HH:mm', day: 'MMM dd', week: 'MMM dd', month: 'MMM<y_bin_46>', quarter: 'qqq<y_bin_46>', year: 'yyyy' }
                            },
                            ticks: { ...defaultChartJsOptions.scales.x.ticks, maxTicksLimit: 6 }
                        },
                        y: { ...defaultChartJsOptions.scales.y, beginAtZero: false, ticks: { ...defaultChartJsOptions.scales.y.ticks, callback: (v) => v.toFixed(1) } }
                    }
                 }
            };
            if (window.cpiChartInstance) { try { window.cpiChartInstance.destroy(); } catch(e) { console.error("Error destroying previous CPI chart", e); } }
            cpiChart = new Chart(cpiChartCanvas, config); window.cpiChartInstance = cpiChart;
        }
     }

     function updateD3Treemap(allItemsData) {
        if (!marketCapTreemapContainer) { return; }
        if (!Array.isArray(allItemsData)) { return; }

        const treemapData = allItemsData
            .map(item => ({
                name: item.name || 'Unknown',
                value: item.price ?? 0,
                itemData: item
            }))
            .filter(item => item.value > 0)
            .sort((a, b) => b.value - a.value);


        const containerWidth = marketCapTreemapContainer.clientWidth;
        const containerHeight = marketCapTreemapContainer.clientHeight;

        d3.select(marketCapTreemapContainer).select('svg').remove();

        if (treemapData.length === 0 || containerWidth <= 0 || containerHeight <= 0) {
            return;
        }

        const root = d3.hierarchy({ name: "root", children: treemapData })
            .sum(d => d.value)
            .sort((a, b) => b.value - a.value);

        const treemapLayout = d3.treemap()
            .size([containerWidth, containerHeight])
            .padding(1);

        treemapLayout(root);

        const changes = allItemsData.map(item => item.changePercent ?? 0).filter(c => c !== 0);
        const maxPositiveChange = Math.max(0.01, ...changes.filter(c => c > 0));
        const minNegativeChange = Math.min(-0.01, ...changes.filter(c => c < 0));

        const getColorForChange = (change) => {
            const changeNum = parseFloat(change);
            const solidGreen = '5, 150, 105';
            const solidRed = '239, 68, 68';
            const neutralGray = '75, 85, 99';
            const minOpacity = 0.15;
            const maxAbsChange = Math.max(0.01, Math.abs(maxPositiveChange), Math.abs(minNegativeChange));

            if (change === null || change === undefined || isNaN(changeNum) || Math.abs(changeNum) < 0.001) {
                return `rgba(${neutralGray}, ${minOpacity})`;
            }

            const absChangeRatio = Math.abs(changeNum) / maxAbsChange;
            const opacity = Math.min(1, Math.max(minOpacity, absChangeRatio));

            if (changeNum > 0) {
                return `rgba(${solidGreen}, ${opacity})`;
            } else {
                return `rgba(${solidRed}, ${opacity})`;
            }
        };


        const svg = d3.select(marketCapTreemapContainer)
            .append("svg")
            .attr("viewBox", `0 0 ${containerWidth} ${containerHeight}`)
            .attr("preserveAspectRatio", "xMidYMid meet");

        const nodes = svg.selectAll("g")
            .data(root.leaves())
            .enter()
            .append("g")
            .attr("transform", d => `translate(${d.x0},${d.y0})`);

        nodes.append("rect")
            .attr("width", d => d.x1 - d.x0)
            .attr("height", d => d.y1 - d.y0)
            .attr("fill", d => getColorForChange(d.data.itemData.changePercent))
            .attr("stroke", "rgba(255, 255, 255, 0.3)")
            .attr("stroke-width", 0.5);

        nodes.each(function(d) {
                const nodeGroup = d3.select(this);
                const blockWidth = d.x1 - d.x0;
                const blockHeight = d.y1 - d.y0;
                const minDimForIcon = 20;
                const minIconSize = 12;
                const maxIconSize = 36;
                const textHeight = 12;
                const iconPadding = 4;

                if (blockWidth > minDimForIcon && blockHeight > (minDimForIcon + textHeight + iconPadding)) {
                    const availableWidth = blockWidth - (2 * iconPadding);
                    const availableHeightForIcon = blockHeight - textHeight - iconPadding;
                    const smallerDim = Math.min(availableWidth, availableHeightForIcon);
                    let targetIconSize = Math.max(minIconSize, Math.min(maxIconSize, smallerDim * 0.7));

                    let targetFontSize = Math.max(8, Math.min(11, availableHeightForIcon * 0.2, availableWidth * 0.15));

                    if (targetIconSize + targetFontSize + iconPadding > blockHeight) {
                        const scaleDown = blockHeight / (targetIconSize + targetFontSize + iconPadding);
                        targetIconSize *= (scaleDown * 0.9);
                        targetFontSize *= (scaleDown* 0.9);
                        targetIconSize = Math.max(minIconSize, targetIconSize);
                        targetFontSize = Math.max(8, targetFontSize);
                    }

                     if (blockWidth < targetIconSize || blockHeight < (targetIconSize + targetFontSize + iconPadding)) {
                         return;
                     }

                    nodeGroup.append("image")
                        .attr('xlink:href', `${API_BASE_URL}/icons/${d.data.itemData.identifier}.png`)
                        .attr('x', (blockWidth - targetIconSize) / 2)
                        .attr('y', (blockHeight - targetIconSize - targetFontSize - iconPadding) / 2)
                        .attr('width', targetIconSize)
                        .attr('height', targetIconSize);

                    nodeGroup.append("text")
                        .attr("class", "treemap-label")
                        .attr("x", blockWidth / 2)
                        .attr("y", (blockHeight + targetIconSize - targetFontSize) / 2 + iconPadding)
                        .attr("font-size", `${targetFontSize}px`)
                        .attr("fill", "#ffffff")
                        .attr("text-anchor", "middle")
                        .attr("dominant-baseline", "middle")
                        .text(formatTrend(d.data.itemData.changePercent, true, ''));
                }
             });


        const tooltip = d3.select(treemapTooltip);

        nodes.on("mouseover", (event, d) => {
            tooltip.style("display", "block");
        })
        .on("mousemove", (event, d) => {
            const itemData = d.data.itemData;
            const price = itemData?.price ?? 0;
            const change = itemData?.changePercent;
            const name = itemData?.name ?? 'Unknown';

            tooltip.html(`
                <div class="font-semibold">${name}</div>
                <div>Price: ${formatCurrency(price)}</div>
                <div>Change: <span class="${getTrendClass(change)}">${formatTrend(change, true, '-')}</span></div>
            `)
            .style("left", (event.pageX + 10) + "px")
            .style("top", (event.pageY - 10) + "px");
        })
        .on("mouseout", () => {
            tooltip.style("display", "none");
        });

    }

    async function pollData() {
        try {
            const newItemsData = await fetchData('/items');
            if (!Array.isArray(newItemsData)) {
                return;
            }

            const priceChangesMap = new Map();
            newItemsData.forEach(newItem => {
                const identifier = String(newItem.identifier);
                const oldPrice = previousPrices.get(identifier);
                const newPrice = newItem.price;
                let direction = 'none';
                if (oldPrice !== undefined && newPrice !== undefined && newPrice !== oldPrice) {
                    direction = newPrice > oldPrice ? 'up' : 'down';
                }
                priceChangesMap.set(identifier, direction);
            });

            allItems = newItemsData;

            sortAndRenderItems(priceChangesMap);
            updateD3Treemap(allItems);

            if (selectedItemIdentifier) {
                const selectedItem = allItems.find(item => String(item.identifier) === selectedItemIdentifier);
                const changeDirection = priceChangesMap.get(selectedItemIdentifier);
                if (selectedItem) {

                    const newPriceText = formatCurrency(selectedItem.price, '-');
                    const newChangeText = formatTrend(selectedItem.changePercent, true, '-');
                    const newChangeClass = getTrendClass(selectedItem.changePercent);

                    if (currentPriceElement.textContent !== newPriceText) {
                        currentPriceElement.textContent = newPriceText;
                         if (changeDirection && changeDirection !== 'none' && currentPriceElement.parentNode) {
                             applyFlashAnimation(currentPriceElement.parentNode, changeDirection);
                         }
                    }

                    if (change1hElement.textContent !== newChangeText || !change1hElement.classList.contains(newChangeClass)) {
                         change1hElement.textContent = newChangeText;
                         change1hElement.className = `text-lg font-semibold ${newChangeClass}`;
                          if (changeDirection && changeDirection !== 'none' && change1hElement.parentNode) {
                              applyFlashAnimation(change1hElement.parentNode, changeDirection);
                          }
                    }

                    const sortedByPrice = [...allItems]
                        .filter(item => item && item.price !== null && item.price !== undefined && !isNaN(item.price))
                        .sort((a, b) => b.price - a.price);
                    const rankIndex = sortedByPrice.findIndex(item => String(item?.identifier) === selectedItemIdentifier);
                    const calculatedRank = rankIndex !== -1 ? rankIndex + 1 : null;
                    marketRankElement.textContent = calculatedRank ? `#${calculatedRank}` : '-';
                }
            }

            previousPrices = new Map(allItems.map(item => [String(item.identifier), item.price]));

        } catch (error) {
            console.error("Polling failed:", error);
        }
    }

    async function initialize() {
        const requiredElementIds = [
            'item-list', 'search-input', 'selected-item-icon', 'selected-item-name',
            'selected-item-description', 'current-price', 'sell-price-display',
            'buy-price-display', 'item-price-chart-container',
            'cpi-chart', 'treemap-container',
            'inflation-adjust-checkbox', 'log-scale-checkbox', 'market-rank',
            'all-time-high', 'all-time-low', 'reset-zoom-button',
            'chart-loading-spinner', 'change-1h', 'inception-return',
            'sort-controls', 'treemap-tooltip',
            'volatility', 'max-drawdown', 'top-portfolios-container'
        ];
        const missingElement = requiredElementIds.find(id => !document.getElementById(id));
        if (missingElement) {
             console.error(`Critical UI element missing: #${missingElement}. Aborting initialization.`);
             document.body.innerHTML = '<div class="p-4 text-red-500">Error: UI elements missing. Cannot initialize application.</div>';
             return;
         }
        initializeLightweightChart();

        try {
            const [itemsData, fetchedCpiData, popularItemData, topPortfoliosData] = await Promise.all([
                fetchData('/items').catch(e => { console.error("Failed to load items", e); return []; }),
                fetchData('/charts/cpi').catch(e => { console.error("Failed to load CPI data", e); return []; }),
                fetchData('/popular-item').catch(e => { console.error("Failed to load popular item", e); return null; }),
                fetchData('/top-portfolios').catch(e => { console.error("Failed to load top portfolios", e); return []; })
            ]);

            allItems = Array.isArray(itemsData) ? itemsData : [];
            previousPrices = new Map(allItems.map(item => [String(item.identifier), item.price]));
            sortAndRenderItems();


            if (allItems.length > 0) { updateD3Treemap(allItems); }
            else { console.warn("No item data available for Treemap."); }

            if (Array.isArray(fetchedCpiData) && fetchedCpiData.length > 0) {
                cpiDataStore = fetchedCpiData
                    .filter(dp => dp.time !== null && dp.time !== undefined && !isNaN(dp.time) && dp.value !== null && dp.value !== undefined && !isNaN(dp.value))
                    .sort((a, b) => a.time - b.time);
                updateCpiChart(fetchedCpiData);
            } else {
                cpiDataStore = []; updateCpiChart([]);
            }

             renderTopPortfolios(topPortfoliosData);

             let initialItemIdentifier = null;
             if (popularItemData && popularItemData?.identifier) { initialItemIdentifier = String(popularItemData.identifier); }
             else if (allItems.length > 0 && allItems[0]?.identifier) { initialItemIdentifier = String(allItems[0].identifier); }

             if (initialItemIdentifier && lightweightChart) {
                 await handleItemSelection(initialItemIdentifier);
             } else if (!lightweightChart) {
                  updateSelectedItemDetails(null, null, null, null, null, null, null);
                  selectedItemDescElement.textContent = "Chart failed to load. Select an item.";
             } else {
                 updateSelectedItemDetails(null, null, null, null, null, null, null);
                 clearLightweightChart();
                 selectedItemDescElement.textContent = "No items loaded or first item lacks identifier.";
             }

             searchInputElement.addEventListener('input', () => sortAndRenderItems());
             inflationCheckbox.addEventListener('change', handleInflationToggle);
             logScaleCheckbox.addEventListener('change', handleLogScaleToggle);
             resetButton.addEventListener('click', handleResetZoom);
             if (sortControlsContainer) {
                 sortControlsContainer.addEventListener('click', handleSortClick);
             }

             if (pollingIntervalId) clearInterval(pollingIntervalId);
             pollingIntervalId = setInterval(pollData, POLLING_INTERVAL);


        } catch (error) {
            console.error("Initialization failed:", error);
            if (itemListElement) { itemListElement.innerHTML = '<li class="p-2 text-red-400">Error loading data. Check console.</li>'; }
        }
        console.log("Initialization complete.");
    }

    function renderTopPortfolios(portfolioData) {
        if (!topPortfoliosContainer) {
            console.error("Top portfolios container not found!");
            return;
        }
         topPortfoliosContainer.innerHTML = '';

        if (!Array.isArray(portfolioData) || portfolioData.length === 0) {
            topPortfoliosContainer.innerHTML = '<p class="text-gray-400 text-center">No portfolio data available.</p>';
            return;
        }

        const cardContainer = document.createElement('div');
        cardContainer.className = 'grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4';

        const top5 = portfolioData.slice(0, 5);

        top5.forEach((portfolio, index) => {
            const card = document.createElement('div');
            card.className = 'portfolio-card bg-gray-700 p-3 rounded-lg flex flex-col items-center text-center border-2 border-transparent';

            const rank = index + 1;
            if (index === 0) card.classList.add('rank-1');
            else if (index === 1) card.classList.add('rank-2');
            else if (index === 2) card.classList.add('rank-3');

            const ownerName = portfolio.ownerName || 'Unknown Owner';
            const value = portfolio.value;
            const items = Object.entries(portfolio.content || {});
            items.sort(([, qtyA], [, qtyB]) => qtyB - qtyA);
            const top3Items = items.slice(0, 3);
            const remainingCount = items.length - top3Items.length;

            let itemsHTML = '';
            top3Items.forEach(([itemId, quantity]) => {
                itemsHTML += `
                    <div class="inline-flex items-center bg-gray-800/50 px-1.5 py-0.5 rounded text-xs mx-0.5 my-0.5" title="${itemId.replace('_', ' ')}">
                        <img src="/api/icons/${itemId}.png" class="w-3 h-3 mr-1 flex-shrink-0" alt="${itemId}" onerror="this.style.display='none'">
                        <span class="text-gray-300">${quantity}</span>
                    </div>`;
            });
            if (remainingCount > 0) {
                 itemsHTML += `<span class="portfolio-more-items"> and ${remainingCount} more...</span>`;
            }

            card.innerHTML = `
                 <div class="w-full mb-2 text-left">
                      <span class="font-bold text-xl text-gray-500">${rank}</span>
                 </div>
                 <div class="flex flex-col items-center mb-2">
                      <img src="https://mc-heads.net/head/${encodeURIComponent(ownerName)}" alt="${ownerName}'s head" class="w-8 h-8 rounded-md mb-1" onerror="this.style.display='none'">
                      <span class="font-semibold text-indigo-300 text-sm truncate max-w-[100px]" title="${ownerName}">${ownerName}</span>
                 </div>
                 <div class="text-lg font-bold text-white mb-2">${formatCurrency(value)}</div>
                 <div class="flex flex-wrap justify-center gap-1 mt-auto pt-2 border-t border-gray-600 w-full min-h-[30px]">
                     ${itemsHTML || '<span class="text-xs text-gray-500">Empty</span>'}
                 </div>
            `;
            cardContainer.appendChild(card);
        });

        topPortfoliosContainer.appendChild(cardContainer);
    }


    initialize();

});
