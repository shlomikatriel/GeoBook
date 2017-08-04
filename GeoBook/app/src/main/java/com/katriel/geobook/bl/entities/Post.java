package com.katriel.geobook.bl.entities;


public class Post {
	private String uid;
	private long timeStamp;
	private String content;
	private double lat;
	private double lng;

	public Post(String uid, long timeStamp, String content, double lat, double lng) {
		this.uid = uid;
		this.timeStamp = timeStamp;
		this.content = content;
		this.lat = lat;
		this.lng = lng;
	}

	public String getUid() {
		return uid;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public String getContent() {
		return content;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Post post = (Post) o;

		if (timeStamp != post.timeStamp) return false;
		return uid != null ? uid.equals(post.uid) : post.uid == null;

	}

	@Override
	public int hashCode() {
		int result = uid != null ? uid.hashCode() : 0;
		result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
		return result;
	}
}
