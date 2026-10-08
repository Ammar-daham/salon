package com.example.salon.paging;

import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;

/** Which page of a list to answer with (BE-15): page counts from 1, size is 1 to 100 rows. */
public record PageQuery(int page, int size)
{
	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	public PageQuery
	{
		if (page < 1)
			throw new BaseException("page must be 1 or more", ErrorCode.BAD_REQUEST);
		if (size < 1 || size > MAX_SIZE)
			throw new BaseException("size must be between 1 and " + MAX_SIZE, ErrorCode.BAD_REQUEST);
	}

	/** From the ?page and ?size of a request, either of which may be left out. */
	public static PageQuery of(Integer page, Integer size)
	{
		return new PageQuery(page != null ? page : 1, size != null ? size : DEFAULT_SIZE);
	}

	public long offset()
	{
		return (long) (page - 1) * size;
	}
}
