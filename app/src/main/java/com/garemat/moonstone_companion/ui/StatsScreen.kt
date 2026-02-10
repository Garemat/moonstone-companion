package com.garemat.moonstone_companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garemat.moonstone_companion.*
import com.garemat.moonstone_companion.ui.theme.LocalAppTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    viewModel: CharacterViewModel
) {
    val results by viewModel.gameResults.collectAsState()
    val isMoonstone = LocalAppTheme.current == AppTheme.MOONSTONE

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No games played yet!", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(results) { result ->
                    GameResultBanner(result)
                }
            }
        }
    }
}

@Composable
fun GameResultBanner(result: GameResult) {
    val isMoonstone = LocalAppTheme.current == AppTheme.MOONSTONE
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = if (isMoonstone) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            // Quadrant Backgrounds with Borders and Faction Images
            QuadrantLayout(result.playerStats, result.winnerIndex)

            // Center Score Circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1976D2)) // Blue Circle
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                when (result.playerStats.size) {
                    2 -> {
                        Text(
                            text = "${result.playerStats[0].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                        )
                        Text(
                            text = "${result.playerStats[1].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                        )
                    }
                    3 -> {
                        Text(
                            text = "${result.playerStats[0].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp)
                        )
                        Text(
                            text = "${result.playerStats[1].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 8.dp)
                        )
                        Text(
                            text = "${result.playerStats[2].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        )
                    }
                    4 -> {
                        Text(
                            text = "${result.playerStats[0].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp)
                        )
                        Text(
                            text = "${result.playerStats[1].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 8.dp)
                        )
                        Text(
                            text = "${result.playerStats[2].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)
                        )
                        Text(
                            text = "${result.playerStats[3].totalStones}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp)
                        )
                    }
                }
            }

            // Player Info Overlays
            PlayerStatsOverlay(result)
            
            // Timestamp
            val date = remember(result.timestamp) {
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(result.timestamp))
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
            )
        }
    }
}

@Composable
fun PlayerQuadrant(stat: PlayerStat, isWinner: Boolean, modifier: Modifier) {
    val isMoonstone = LocalAppTheme.current == AppTheme.MOONSTONE
    val context = LocalContext.current
    val backgroundRes = remember(stat.faction) {
        val resName = when(stat.faction) {
            Faction.COMMONWEALTH -> "commonwealth"
            Faction.DOMINION -> "dominion"
            Faction.LESHAVULT -> "leshavult"
            Faction.SHADES -> "shades"
        }
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    Box(
        modifier = modifier
            .background(getFactionColor(stat.faction))
            .then(
                if (isWinner) Modifier.border(2.dp, Color(0xFFFFD700))
                else Modifier.border(1.dp, Color.White)
            )
    ) {
        if (isMoonstone && backgroundRes != 0) {
            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.3f),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun QuadrantLayout(stats: List<PlayerStat>, winnerIndex: Int?) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            // Player 0
            PlayerQuadrant(stats[0], isWinner = winnerIndex == 0, modifier = Modifier.weight(1f).fillMaxHeight())
            // Player 1
            if (stats.size > 1) {
                PlayerQuadrant(stats[1], isWinner = winnerIndex == 1, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        if (stats.size > 2) {
            if (stats.size == 3) {
                // Player 3 occupies the whole bottom half
                PlayerQuadrant(stats[2], isWinner = winnerIndex == 2, modifier = Modifier.weight(1f).fillMaxWidth())
            } else {
                Row(modifier = Modifier.weight(1f)) {
                    // Player 2
                    PlayerQuadrant(stats[2], isWinner = winnerIndex == 2, modifier = Modifier.weight(1f).fillMaxHeight())
                    // Player 3
                    if (stats.size > 3) {
                        PlayerQuadrant(stats[3], isWinner = winnerIndex == 3, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerStatsOverlay(result: GameResult) {
    val stats = result.playerStats
    val winnerIdx = result.winnerIndex

    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        if (stats.size == 2) {
            // 2 Player Specific Refined Layout
            PlayerInfoRefined(stats[0], isWinner = winnerIdx == 0, modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.45f))
            PlayerInfoRefined(stats[1], isWinner = winnerIdx == 1, modifier = Modifier.align(Alignment.TopEnd).fillMaxWidth(0.45f))
        } else {
            // Player 1
            PlayerInfo(stats[0], isWinner = winnerIdx == 0, modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.45f))
            
            // Player 2
            if (stats.size > 1) {
                PlayerInfo(stats[1], isWinner = winnerIdx == 1, modifier = Modifier.align(Alignment.TopEnd).fillMaxWidth(0.45f), textAlign = TextAlign.End)
            }

            // Player 3
            if (stats.size == 3) {
                PlayerInfo(stats[2], isWinner = winnerIdx == 2, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.9f), textAlign = TextAlign.Center)
            } else if (stats.size > 3) {
                PlayerInfo(stats[2], isWinner = winnerIdx == 2, modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.45f))
                PlayerInfo(stats[3], isWinner = winnerIdx == 3, modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.45f), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun PlayerInfoRefined(stat: PlayerStat, isWinner: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.padding(4.dp), // Padding inside the quadrant border
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val displayName = if (!stat.playerName.isNullOrBlank()) "${stat.playerName} - ${stat.troupeName}" else stat.troupeName
        Text(
            text = displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val charNames = stat.characterStats.joinToString(", ") { it.name }
        Text(
            text = charNames,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun PlayerInfo(stat: PlayerStat, isWinner: Boolean, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = when(textAlign) {
            TextAlign.End -> Alignment.End
            TextAlign.Center -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
    ) {
        val displayName = if (!stat.playerName.isNullOrBlank()) "${stat.playerName} - ${stat.troupeName}" else stat.troupeName
        Text(
            text = displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val charNames = stat.characterStats.joinToString(", ") { it.name }
        Text(
            text = charNames,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
    }
}
