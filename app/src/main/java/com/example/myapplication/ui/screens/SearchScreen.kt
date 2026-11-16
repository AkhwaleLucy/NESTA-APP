package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.*
import com.example.myapplication.repository.NestaRepository
import com.example.myapplication.ui.components.FilterBottomSheet
import com.example.myapplication.ui.components.PropertyCard
import com.example.myapplication.ui.components.PropertyMapView
import com.example.myapplication.ui.theme.*

@Composable
fun SearchScreen(
    initialFilter: SearchFilter = SearchFilter(),
    properties: List<Property>,
    favoriteIds: Set<String>,
    compareIds: Set<String>,
    onPropertyClick: (Property) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onCompareToggle: (String) -> Unit
) {
    var filterState by remember { mutableStateOf(initialFilter) }
    var searchQuery by remember { mutableStateOf(initialFilter.query) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Update filter when query changes with smart parser
    fun applyQuerySearch(q: String) {
        val parsed = NestaRepository.parseNaturalSearch(q)
        filterState = parsed
    }

    val filteredList = remember(filterState, properties) {
        NestaRepository.getFilteredProperties(filterState)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
    ) {
        // --- SEARCH HEADER ---
        Surface(
            color = WarmOffWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Search Input Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        applyQuerySearch(it)
                    },
                    placeholder = { Text("Search location, type, price... (e.g., 2 bed Kilimani)", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedTerracotta) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                filterState = SearchFilter()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MutedText)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepNavy,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Actions Bar: Filter Button, View Switcher (List / Map), Sort Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter Button
                        Button(
                            onClick = { showFilterSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Filter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmOffWhite)
                        }

                        // Map / List Toggle Button
                        OutlinedButton(
                            onClick = { isMapView = !isMapView },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isMapView) Icons.Default.List else Icons.Default.Place,
                                contentDescription = null,
                                tint = DarkCharcoal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMapView) "List View" else "Map View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkCharcoal
                            )
                        }
                    }

                    // Sort Dropdown Box
                    Box {
                        TextButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = filterState.sortBy.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedTerracotta
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortBy.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label, fontSize = 13.sp) },
                                    onClick = {
                                        filterState = filterState.copy(sortBy = sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Active Filters Chips Row
                if (filterState.propertyType != null || filterState.neighborhood != null || filterState.listingType != null || filterState.maxPrice != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        filterState.listingType?.let { type ->
                            item { ActiveChip(text = type.displayName) { filterState = filterState.copy(listingType = null) } }
                        }
                        filterState.propertyType?.let { type ->
                            item { ActiveChip(text = type.displayName) { filterState = filterState.copy(propertyType = null) } }
                        }
                        filterState.neighborhood?.let { hood ->
                            item { ActiveChip(text = hood) { filterState = filterState.copy(neighborhood = null) } }
                        }
                        filterState.maxPrice?.let { maxP ->
                            item { ActiveChip(text = "< KSh ${maxP.toInt()}") { filterState = filterState.copy(maxPrice = null) } }
                        }
                    }
                }
            }
        }

        // --- RESULTS SECTION ---
        if (isMapView) {
            PropertyMapView(
                properties = filteredList,
                onPropertySelect = onPropertyClick,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${filteredList.size} Properties Found in Nairobi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MutedText,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No properties match your filter", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Try adjusting your price range, location, or bedrooms", fontSize = 12.sp, color = MutedText)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    filterState = SearchFilter()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                            ) {
                                Text("Reset Filters", color = WarmOffWhite)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(filteredList) { prop ->
                            PropertyCard(
                                property = prop,
                                isFavorite = favoriteIds.contains(prop.id),
                                isCompared = compareIds.contains(prop.id),
                                onFavoriteToggle = { onFavoriteToggle(prop.id) },
                                onCompareToggle = { onCompareToggle(prop.id) },
                                onClick = { onPropertyClick(prop) }
                            )
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = filterState,
                onApplyFilter = { newFilter -> filterState = newFilter },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
fun ActiveChip(text: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = WarmIvory,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove",
                tint = DarkCharcoal,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onRemove() }
            )
        }
    }
}
