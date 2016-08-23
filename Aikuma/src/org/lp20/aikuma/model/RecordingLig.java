package org.lp20.aikuma.model;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.ui.RecordingMetadataLig;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma.util.StandardDateFormat;

import android.util.Log;

public class RecordingLig extends Recording {
	
	public static final String TAG = "RecordingLig";
	public static final String RECORDINGS = "recordings/";
	public static final String WAV_EXT = ".wav";
	public static final String PRVW_WAV_EXT = SAMPLE_SUFFIX;
	public static final String MAP_EXT = ".map";
	
	private Language recordLang;
	private Language motherTong;
	private String regionOrigin;
	private String speakerName;
	private int speakerAge = 0;
	private String speakerGender;
	
	public RecordingLig(UUID recordingUUID, String name, Date date,
			String versionName, String ownerId, Language recordLang, Language motherTong,
			List<Language> languages, List<String> speakersIds,
			String deviceName, String androidID, String groupId, String sourceVerId,
			long sampleRate, int durationMsec, String format, int numChannels, 
			int bitsPerSample, Double latitude, Double longitude,
			String region, String spkrName, int spkrAge, String spkrGndr) {
		super(recordingUUID, name, date, versionName, ownerId,
				languages, speakersIds, deviceName, androidID, groupId, 
				sourceVerId, sampleRate, durationMsec, format, numChannels, 
				bitsPerSample, latitude, longitude);
		this.regionOrigin = region;
		this.speakerName = spkrName;
		this.speakerAge = spkrAge;
		this.speakerGender = spkrGndr;
		this.recordLang = recordLang != null ? new Language(recordLang.getName(),recordLang.getCode()) 
											: new Language("", "");
		this.motherTong = motherTong != null ? new Language(motherTong.getName(),motherTong.getCode()) 
											: new Language("", "");
	}
	
	public RecordingLig(UUID recordingUUID, String name, Date date,
			String versionName, String ownerId,
			List<Language> languages, List<String> speakersIds,
			String deviceName, String androidID, String groupId, String sourceVerId,
			long sampleRate, int durationMsec, String format, int numChannels, 
			int bitsPerSample, Double latitude, Double longitude,
			String region, String spkrName, int spkrAge, String spkrGndr) {
		
		super(recordingUUID, name, date, versionName, ownerId,
			languages, speakersIds, deviceName, androidID, groupId, 
			sourceVerId, sampleRate, durationMsec, format, numChannels, 
			bitsPerSample, latitude, longitude);
		this.regionOrigin = region;
		this.speakerName = spkrName;
		this.speakerAge = spkrAge;
		this.speakerGender = spkrGndr;
	}
	
	/**
	 * Public constructor from super class Recording
	 * for existing recordings
	 * @param the original recording
	 * @return an instance of RecordingLig
	 */
	public RecordingLig(Recording r) {
		super(r.name, r.date, r.versionName, r.ownerId, r.sourceVerId, 
				r.languages, r.speakersIds, r.androidID, r.groupId, 
				r.respeakingId, r.sampleRate, r.durationMsec, r.format, r.fileType);
	}

	// Moves a WAV file with a temporary UUID from a no-sync directory to
	// its rightful place in the connected world of Aikuma, with a proper name
	// and where it will find it's best friend - a JSON metadata file.
	private void importWav(UUID wavUUID, String ext) throws IOException {
		importWav(wavUUID + ".wav", ext);
	}
	
	private void importWav(String wavUUIDExt, String ext)
			throws IOException {
		File wavFile = new File(getNoSyncRecordingsPath(), wavUUIDExt);
		Log.i(TAG, "importwav: " + wavFile.length());
		File destFile = new File(getIndividualRecordingPath(), this.name + ext);
		FileUtils.moveFile(wavFile, destFile);
//		FileUtils.moveFile(wavFile, this.getFile(idExt));
		Log.i(TAG, "src file " + wavFile.getAbsolutePath() + " moves to " + destFile.getAbsolutePath());
	}

	// Similar to importWav, except for the mapping file.
	private void importMapping(UUID wavUUID, String id)
			throws IOException {
		Log.i(TAG, "importmapping - started");
		File mapFile = new File(getNoSyncRecordingsPath(), wavUUID + ".map");
		Log.i(TAG, "importmapping - map file " + mapFile.getAbsolutePath() + " and exists: " + mapFile.exists());
		File destFile = new File(getIndividualRecordingPath(), this.name + MAP_EXT);
		Log.i(TAG, "importmapping - dest file: " + destFile.getAbsolutePath());
		FileUtils.moveFile(mapFile, destFile);
		Log.i(TAG, "mapping file " + mapFile.getAbsolutePath() + " moves to " + destFile.getAbsolutePath());
	}
	
	public void write() throws IOException {
		// Ensure the directory exists
		File dir = getIndividualRecordingPath();
		dir.mkdir();
		Log.i(TAG, "start write - iniate directory " + dir.getAbsolutePath());
		// Import the wave file into the new recording directory.
		if(this.isMovie()) {
			// TODO update importMov
			super.importMov(recordingUUID, getId());
		} else {
			this.importWav(recordingUUID, WAV_EXT);
			
		}

		// if the recording is original
		if (isOriginal()) {
			// Import the sample wave file into the new recording directory
			importWav(recordingUUID + SAMPLE_SUFFIX, PRVW_WAV_EXT);
		} else {
			// Try and import the mapping file
			importMapping(recordingUUID, getId());
			
			// Write the index file
			super.index(sourceVerId, getVersionName() + "-" + getId());
		}

		JSONObject encodedRecording = this.encode();


		// Write the json metadata.
		File metadataFile = new File(getIndividualRecordingPath(),
				this.name + METADATA_SUFFIX);
		FileIO.writeJSONObject(metadataFile,encodedRecording);
		Log.i(TAG, "Saved metadata file to " + metadataFile.getAbsolutePath());
//		FileIO.writeJSONObject(new File(
//				getRecordingsPath(), getGroupId() + "/" +
//						id + METADATA_SUFFIX),
//				encodedRecording);
		
	}

	/**
	 * Read a recording from the file containing JSON describing the Recording
	 * LIG version
	 *
	 * @param	metadataFile	The file containing the metadata of the recording.
	 * @return	A Recording object corresponding to the json file.
	 * @throws	IOException	If the recording metadata cannot be read.
	 */
	public static RecordingLig read(File metadataFile) throws IOException {
		JSONObject jsonObj = FileIO.readJSONObject(metadataFile);		
		RecordingLig recording = new RecordingLig(read(jsonObj));
		String code = (String) jsonObj.get(RecordingMetadataLig.metaRecordLang);
		recording.recordLang = code.isEmpty() ? new Language(Aikuma.getLanguageCodeMap().get(code), code) : null;
		code = (String) jsonObj.get(RecordingMetadataLig.metaMotherTong);
		recording.motherTong = code.isEmpty() ? new Language(Aikuma.getLanguageCodeMap().get(code), code) : null;
		recording.regionOrigin = (String) jsonObj.get(RecordingMetadataLig.metaOrigin);
		recording.speakerName = (String) jsonObj.get(RecordingMetadataLig.metaSpkrName);
		long age = (Long) jsonObj.get(RecordingMetadataLig.metaSpkrAge);
		recording.speakerAge= (int) age;
		recording.speakerGender = (String) jsonObj.get(RecordingMetadataLig.metaSpkrGender);
		return recording;
	}
	
	public JSONObject encode() {
		JSONObject encodedRecording = super.encode();
		// TODO complete with new metadata
		encodedRecording.put(RecordingMetadataLig.metaOrigin, regionOrigin);
		encodedRecording.put(RecordingMetadataLig.metaSpkrName, speakerName);
		encodedRecording.put(RecordingMetadataLig.metaSpkrAge, speakerAge);
		encodedRecording.put(RecordingMetadataLig.metaSpkrGender, speakerGender);
		encodedRecording.put(RecordingMetadataLig.metaRecordLang, this.recordLang.getCode());
		encodedRecording.put(RecordingMetadataLig.metaMotherTong, this.motherTong.getCode());
		Log.i(TAG, "encoding metadata into json format");
		return encodedRecording;
	}

	/**
	 * Returns a File that refers to the actual recording file.
	 * LIG version
	 *
	 * @return	The file the recording is stored in.
	 */
	public File getFile() {
		String extension = (this.isMovie())? ".mp4" : ".wav";
		return new File(getIndividualRecordingPath(), this.name + extension);
//		return getFile(id + extension);
	}
	
	public File getRecordingsPath() {
		File path = new File(
				FileIO.getOwnerPath(), RECORDINGS);
		path.mkdirs();
		return path;
	}
	
	public File getIndividualRecordingPath() {
		File path;
		if (this.name.contains("respeak") || this.name.contains("translate")) {
			String[] dir = this.name.split("_");
			path = new File(getRecordingsPath(), 
					dir[0] + "_" + dir[1] + "_" + dir[2] + "/");
		} else { path = new File(getRecordingsPath(), this.name + "/"); }
		path.mkdirs();
		return path;
	}

}
