package com.katriel.geobook.bl.entities;

public class User {
	private String uid;
	private String email;
	private String displayName;


	public User(String uid, String email, String displayName) {
		this.uid = uid;
		this.email = email;
		this.displayName = displayName;
	}

	public User(User other) {
		this.uid = other.getUid();
		this.email = other.getEmail();
		this.displayName = other.getDisplayName();
	}

	public String getUid() {
		return uid;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		User user = (User) o;

		return uid != null ? uid.equals(user.uid) : user.uid == null;

	}

	@Override
	public int hashCode() {
		return uid != null ? uid.hashCode() : 0;
	}
}
