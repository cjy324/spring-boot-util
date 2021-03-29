package com.jhs.springBoot.util.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.jhs.springBoot.util.dto.Attr;

@Mapper
public interface AttrDao {
	Attr get(@Param("relTypeCode") String relTypeCode, @Param("relId") int relId, @Param("typeCode") String typeCode, @Param("type2Code") String type2Code);

	int remove(@Param("relTypeCode") String relTypeCode, @Param("relId") int relId, @Param("typeCode") String typeCode, @Param("type2Code") String type2Code);

	int setValue(@Param("relTypeCode") String relTypeCode, @Param("relId") int relId, @Param("typeCode") String typeCode, @Param("type2Code") String type2Code, @Param("value") String value, @Param("expireDate") String expireDate);

	String getValue(@Param("relTypeCode") String relTypeCode, @Param("relId") int relId, @Param("typeCode") String typeCode, @Param("type2Code") String type2Code);

	// 테스트 추가
	String getExpireDate(@Param("relTypeCode") String relTypeCode, @Param("relId") int relId, @Param("typeCode") String typeCode, @Param("type2Code") String type2Code, @Param("value") String value);
}
