
    val areaValue = acres.toDoubleOrNull() ?: 0.0
    val costValue = totalCost.toDoubleOrNull() ?: 0.0
    val yieldValue = tonnesPerAcre.toDoubleOrNull() ?: 0.0
    val rateValue = rate.toDoubleOrNull() ?: 0.0
    val revenue = areaValue * yieldValue * rateValue
    val profit = revenue - costValue

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("खर्च व नफा कॅल्क्युलेटर", style = MaterialTheme.typography.titleLarge)
        Text("उत्पादन व दर हे तुम्ही भरलेले अंदाज आहेत.")

        NumberField("क्षेत्र (एकर)", acres) { acres = it }
        NumberField("एकूण खर्च (₹)", totalCost) { totalCost = it }
        NumberField("उत्पादन (टन प्रति एकर)", tonnesPerAcre) { tonnesPerAcre = it }
        NumberField("दर (₹ प्रति टन)", rate) { rate = it }

        Card(colors = CardDefaults.cardColors(containerColor = paleGreen)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("अंदाजित एकूण उत्पन्न")
                Text(
                    "₹ ${money(revenue)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = green,
                    fontWeight = FontWeight.Bold
                )
                Text("अंदाजित नफा / तोटा")
                Text(
                    "₹ ${money(profit)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (profit >= 0) green else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text("एकरी खर्च: ₹ ${money(if (areaValue > 0) costValue / areaValue else 0.0)}")
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValue: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun money(value: Double): String =
    String.format(Locale("en", "IN"), "%,.0f", value)

@Composable
private fun NotesScreen(
    prefs: android.content.SharedPreferences
) {
    var note by remember { mutableStateOf("") }
    var allNotes by remember {
        mutableStateOf(prefs.getString("notes", "") ?: "")
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("शेत नोंदवही", style = MaterialTheme.typography.titleLarge)
        Text("आजचे निरीक्षण, काम किंवा पुढील आठवण लिहा.")

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("नोंद लिहा") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
        )

        Button(
            onClick = {
                if (note.isNotBlank()) {
                    val stamp = SimpleDateFormat(
                        "dd-MM-yyyy HH:mm",
                        Locale("mr", "IN")
                    ).format(Date())

                    allNotes = "• $stamp — ${note.trim()}\n$allNotes"
                    prefs.edit().putString("notes", allNotes).apply()
                    note = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("नोंद जतन करा")
        }

        HorizontalDivider()
        Text("जतन केलेल्या नोंदी", fontWeight = FontWeight.Bold)

        if (allNotes.isBlank()) Text("अजून कोणतीही नोंद नाही.")
        else Text(allNotes)

        OutlinedButton(
            onClick = {
                allNotes = ""
                prefs.edit().remove("notes").apply()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("सर्व नोंदी पुसा")
        }
    }
}
