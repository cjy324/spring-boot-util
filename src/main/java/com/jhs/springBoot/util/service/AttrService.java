package com.jhs.springBoot.util.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jhs.springBoot.util.dao.AttrDao;
import com.jhs.springBoot.util.dto.Attr;

@Service
public class AttrService {
	@Autowired
	private AttrDao attrDao;
	private static final String DIV_STR = "___";

	public Attr get(String name) {
		String[] nameBits = name.split(DIV_STR);
		String relTypeCode = nameBits[0];
		int relId = Integer.parseInt(nameBits[1]);
		String typeCode = nameBits[2];
		String type2Code = nameBits[3];

		return get(relTypeCode, relId, typeCode, type2Code);
	}

	public Attr get(String relTypeCode, int relId, String typeCode, String type2Code) {
		return attrDao.get(relTypeCode, relId, typeCode, type2Code);
	}

	public int setValue(String name, String value, String expireDate) {
		String[] nameBits = name.split(DIV_STR);
		String relTypeCode = nameBits[0];
		int relId = Integer.parseInt(nameBits[1]);
		String typeCode = nameBits[2];
		String type2Code = nameBits[3];

		return setValue(relTypeCode, relId, typeCode, type2Code, value, expireDate);
	}

	public String getValue(String name) {
		String[] nameBits = name.split(DIV_STR);
		String relTypeCode = nameBits[0];
		int relId = Integer.parseInt(nameBits[1]);
		String typeCode = nameBits[2];
		String type2Code = nameBits[3];

		return getValue(relTypeCode, relId, typeCode, type2Code);
	}

	public String getValue(String relTypeCode, int relId, String typeCode, String type2Code) {
		String value = attrDao.getValue(relTypeCode, relId, typeCode, type2Code);

		if (value == null) {
			return "";
		}

		return value;
	}

	public int remove(String name) {
		String[] nameBits = name.split(DIV_STR);
		String relTypeCode = nameBits[0];
		int relId = Integer.parseInt(nameBits[1]);
		String typeCode = nameBits[2];
		String type2Code = nameBits[3];

		return remove(relTypeCode, relId, typeCode, type2Code);
	}

	public int remove(String relTypeCode, int relId, String typeCode, String type2Code) {
		return attrDao.remove(relTypeCode, relId, typeCode, type2Code);
	}

	public int setValue(String relTypeCode, int relId, String typeCode, String type2Code, String value,
			String expireDate) {
		attrDao.setValue(relTypeCode, relId, typeCode, type2Code, value, expireDate);
		Attr attr = get(relTypeCode, relId, typeCode, type2Code);

		if (attr != null) {
			return attr.getId();
		}

		return -1;
	}
	
	
	// 테스트 추가
	public String getExpireDate(String name) {
		String[] nameBits = name.split(DIV_STR);
		String relTypeCode = nameBits[0];
		int relId = Integer.parseInt(nameBits[1]);
		String typeCode = nameBits[2];
		String type2Code = nameBits[3];
		String value = nameBits[4];

		return getExpireDate(relTypeCode, relId, typeCode, type2Code, value);
	}

	// 테스트 추가
	public String getExpireDate(String relTypeCode, int relId, String typeCode, String type2Code, String value) {
		String expireDate = attrDao.getExpireDate(relTypeCode, relId, typeCode, type2Code, value);

		if (expireDate == null) {
			return "";
		}

		return expireDate;
	}
}