# Verify

Verify ingest of OCFL object.

**URL** : `/verify/{id}`

**Method** : `POST`

**Data constraints**

Provide key checksum pairs of entire OCFL manifest.

```json
{
    "[key]": "[checksum]"
}
```

**Data example**

All manifest entries must be included.

```json
{
    "v00001/content/data/400005076.mp3": "610e9c6768c1e41b0f997776861308a1",
    "v00001/content/metadata/400005074_aes57.xml": "1d50396a886425b9d8c7e50461d7b523",
    "v00001/content/data/400005073.wav": "23a8548521d63065d58dcbafb82dbeb3",
    "v00001/content/metadata/400005072_textMD.xml": "6f559e7352abee9d8972447eda23cc84",
    "v00001/content/descriptor/400005067_mets.xml": "bea2ca7c6ec1f1b70ef85265aa396dbd",
    "v00001/content/metadata/400005076_aes57.xml": "563295c50220c295e317cc6f5c37ee8b",
    "v00001/content/metadata/400005073_aes57.xml": "1f982931f12de9309157b4e891e0abc7",
    "v00001/content/metadata/400005079_containerMD.xml": "d5504e692af05bceb4e4760b3700c07f",
    "v00001/content/data/400005069.xml": "ece0d3d596258f4215c3bc35b0b36e34",
    "v00001/content/data/400005078.mp3": "1db61ef8c9f1243b80837d52895d688b",
    "v00001/content/data/400005074.wav": "327b95b2c32f1fa36436c0a12ae5bbf8",
    "v00001/content/data/400005079.zip": "4b59de5306cfa1e3797febec199e9708",
    "v00001/content/metadata/400005070_aes57.xml": "db0b43dbf03894bbdb1ddb53768448d9",
    "v00001/content/data/400005072.adl": "a6e126e393d9917e300394d57756ffac",
    "v00001/content/metadata/400005078_aes57.xml": "ed97471a97dc39a13b951afe7a34fb71",
    "v00001/content/data/400005070.wav": "cfd9b3e993c7b030f4807688cddbd77c",
    "v00001/content/data/400005077.mp3": "61233f404ce85c80a17f521cab99feb5",
    "v00001/content/metadata/400005067_structureMap.xml": "fa511483e64be6ef2b7c9e07543df158",
    "v00001/content/metadata/400005069_textMD.xml": "94c551c460d935f3148ebbd06d5412cb",
    "v00001/content/metadata/400005071_aes57.xml": "cb81e8172c153649be8e5e59236f43c3",
    "v00001/content/metadata/400005077_aes57.xml": "67aba5ae05507a320f701de7adb60633",
    "v00001/content/data/400005075.adl": "60da72142a630e223a205877e29be9c9"
}
```

## Success Response

**Condition** : If object is completely verified.

**Code** : `200 OK`

## Error Responses

**Condition** : If something went wrong on server.

**Code** : `500 INTENRAL SERVER ERROR`

**Content** : `Exception message`

### Or

**Condition** : If request body malformed.

**Code** : `400 Bad Request`

**Content** : `Exception message`

### Or

**Condition** : If inventory.json not found.

**Code** : `404 NOT FOUND`

**Content** : `Exception message`

### Or

**Condition** : Verification failed

**Code** : `409 CONFLICT`

**Content example**

```json
{
    "v00001/content/data/400005076.mp3": {
        "error": "Missing input checksum"
    },
    "v00001/content/metadata/400005074_aes57.xml": {
        "error": "Unknown key"
    },
    "v00001/content/data/400005073.wav": {
        "error": "S3 exception message"
    },
    "v00001/content/metadata/400005072_textMD.xml": {
        "error": "Checksums do not match",
        "expected": "d5504e692af05bceb4e4760b3700c07d",
        "actual": "d5504e692af05bceb4e4760b3700c07f"
    },
    "v00001/content/metadata/400005079_containerMD.xml": {
        "error": "Checksums do not match",
        "expected": "d5504e692af05bceb4e4760b3700c07d",
        "actual": "d5504e692af05bceb4e4760b3700c07f"
    },
    "v00001/content/data/91234314.pdf": {
        "error": "Not found in inventory manifest",
    }
}
```

# Verify Update

Verify update of OCFL object.

**URL** : `/verify/{id}/update`

**Method** : `POST`

**Data constraints**

Provide key checksum pairs of updated OCFL manifest entries.

```json
{
    "[key]": "[checksum]"
}
```

**Data example**

Update manifest entries must be included.

```json
{
    "v00001/content/data/400005076.mp3": "610e9c6768c1e41b0f997776861308a1",
    "v00001/content/metadata/400005074_aes57.xml": "1d50396a886425b9d8c7e50461d7b523",
    "v00001/content/data/400005073.wav": "23a8548521d63065d58dcbafb82dbeb3",
    "v00001/content/metadata/400005072_textMD.xml": "6f559e7352abee9d8972447eda23cc84",
    "v00001/content/descriptor/400005067_mets.xml": "bea2ca7c6ec1f1b70ef85265aa396dbd",
    "v00001/content/metadata/400005076_aes57.xml": "563295c50220c295e317cc6f5c37ee8b",
    "v00001/content/metadata/400005073_aes57.xml": "1f982931f12de9309157b4e891e0abc7",
    "v00001/content/metadata/400005079_containerMD.xml": "d5504e692af05bceb4e4760b3700c07f",
    "v00001/content/data/400005069.xml": "ece0d3d596258f4215c3bc35b0b36e34",
    "v00001/content/data/400005078.mp3": "1db61ef8c9f1243b80837d52895d688b",
    "v00001/content/data/91234314.pdf": "2143c243b80837d5283242395d6ac65d"
}
```

## Success Response

**Condition** : If updated checksums verified.

**Code** : `200 OK`

## Error Responses

**Condition** : If something went wrong on server.

**Code** : `500 INTENRAL SERVER ERROR`

**Content** : `Exception message`

### Or

**Condition** : If request body malformed.

**Code** : `400 Bad Request`

**Content** : `Exception message`

### Or

**Condition** : If inventory.json not found.

**Code** : `404 NOT FOUND`

**Content** : `Exception message`

### Or

**Condition** : Verification failed

**Code** : `409 CONFLICT`

**Content example**

```json
{
    "v00001/content/metadata/400005074_aes57.xml": {
        "error": "Unknown key"
    },
    "v00001/content/data/400005073.wav": {
        "error": "S3 exception message"
    },
    "v00001/content/metadata/400005072_textMD.xml": {
        "error": "Checksums do not match",
        "expected": "d5504e692af05bceb4e4760b3700c07d",
        "actual": "d5504e692af05bceb4e4760b3700c07f"
    },
    "v00001/content/metadata/400005079_containerMD.xml": {
        "error": "Checksums do not match",
        "expected": "d5504e692af05bceb4e4760b3700c07d",
        "actual": "d5504e692af05bceb4e4760b3700c07f"
    },
    "v00001/content/data/91234314.pdf": {
        "error": "Not found in inventory manifest",
    }
}
```