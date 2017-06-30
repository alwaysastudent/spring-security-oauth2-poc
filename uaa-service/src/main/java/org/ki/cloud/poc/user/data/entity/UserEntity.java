package org.ki.cloud.poc.user.data.entity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.constraints.Size;

import org.ki.cloud.poc.user.data.entity.type.UserRoleType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

/**
 * 
 * 
 * @author Karthik Iyer
 *
 */
@Data
@EqualsAndHashCode(exclude = { "id" })
@Entity
@EntityListeners({ AuditingEntityListener.class })
@Table(name = "RI_USER", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "GUID" }, name = "RI_USER_AK_01"),
		@UniqueConstraint(columnNames = {
				"USERNAME" }, name = "RI_USER_AK_02") }, indexes = {
						@Index(name = "RI_USER_IX_01", columnList = "ACTIVE", unique = false) })
@SequenceGenerator(name = "SEQ", initialValue = 1, allocationSize = 500, sequenceName = "RI_USER_SEQ")
public class UserEntity {

	@Id
	@Column(name = "ID")
	// @GeneratedValue(strategy = GenerationType.AUTO)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
	@Setter(AccessLevel.NONE)
	private Long id;

	@Setter(AccessLevel.NONE)
	@Version
	@Column(name = "VERSION", nullable = false)
	private long version;

	@Setter(AccessLevel.NONE)
	@Column(name = "GUID", nullable = false, updatable = false)
	private String guid;

	@Column(name = "USERNAME", nullable = false, insertable = true, updatable = true)
	private String username;

	@Column(name = "PASSWORD", nullable = false, insertable = true, updatable = true)
	private String password;

	@Setter(AccessLevel.NONE)
	@CreatedDate
	@Column(name = "CREATED_AT", nullable = false, insertable = true, updatable = false)
	private LocalDateTime createdAt;

	@Setter(AccessLevel.NONE)
	@LastModifiedDate
	@Column(name = "LAST_MODIFIED_AT", nullable = false, insertable = true, updatable = true)
	private LocalDateTime lastModifiedAt;

	@Column(name = "NAME", nullable = false, insertable = true, updatable = true)
	private String name;

	@Column(name = "PHONE", nullable = false, insertable = true, updatable = true)
	private String phone;

	@Column(name = "ACTIVE", nullable = false, insertable = true, updatable = true)
	private boolean active;

	@ElementCollection(targetClass = UserRoleType.class, fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@CollectionTable(name = "RI_ROLE", joinColumns = @JoinColumn(name = "USER_ID"), uniqueConstraints = @UniqueConstraint(columnNames = {
			"USER_ID",
			"ROLE" }, name = "RI_ROLE_AK_01"), foreignKey = @ForeignKey(name = "RI_ROLE_FK_01"))
	@Column(name = "ROLE")
	@Size(min = 0)
	private Set<UserRoleType> roles;

	public UserEntity(String u, String p, String name, String phone, boolean active,
			Set<UserRoleType> roles) {
		this();
		this.username = u;
		this.password = p;
		this.name = name;
		this.phone = phone;
		this.active = active;
		this.roles = roles;
	}

	public UserEntity() {
		this.guid = UUID.randomUUID().toString();
	}

	public UserEntity addRole(UserRoleType roleType) {
		roles.add(roleType);
		return this;
	}

}
