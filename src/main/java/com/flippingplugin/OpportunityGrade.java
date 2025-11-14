package com.flippingplugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OpportunityGrade
{
	A_PLUS("A+", 90, 100),
	A("A", 80, 89),
	B_PLUS("B+", 70, 79),
	B("B", 60, 69),
	C_PLUS("C+", 50, 59),
	C("C", 40, 49),
	D("D", 30, 39),
	F("F", 0, 29);

	private final String displayName;
	private final int minScore;
	private final int maxScore;

	public static OpportunityGrade fromScore(int score)
	{
		for (OpportunityGrade grade : values())
		{
			if (score >= grade.minScore && score <= grade.maxScore)
			{
				return grade;
			}
		}
		return F;
	}
}
