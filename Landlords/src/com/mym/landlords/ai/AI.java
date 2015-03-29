package com.mym.landlords.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.mym.landlords.card.Bomb;
import com.mym.landlords.card.Card;
import com.mym.landlords.card.CardSuit;
import com.mym.landlords.card.CardType;
import com.mym.landlords.card.Straight;

/**
 * 处理游戏的AI逻辑。
 * <p>所有的字段和方法访问权限均为包级别访问，即外界不可见。方法会通过Player类进行封装，对外界透明。
 * 此外，所有的方法均不做为静态，主要是为了AI的定制化和多样化考虑，可以引入策略模式使得AI更灵活多变。</p>
 * @author Muyangmin
 * @create 2015-3-21
 */
final class AI {
	
	private static final String LOG_TAG = "AI";
	
	//package access
	AI(){}
	
	/**
	 * 按照牌张大小和牌中炸弹数量分析叫牌的分数。
	 * <p>基本算法如下:王炸记8分，大王4分，小王3分，每个2记2分，每个A记1分，炸弹每个记3分。
	 * 全部加起来后看最后得分，大于8分即可叫3分，大于5分可叫2分，大于3分可叫1分，否则不叫。</p>
	 * @return 返回0~3之间的某个数值。
	 */
	private final int callLandlord(ArrayList<Card> list){
		//force sort 
		Collections.sort(list);
		int evaluateScore = 0;
		int size = list.size();
		//判断王炸和单张大小王
		if ( list.get(size-1).getValue()==Card.CARD_VALUE_JOKER_B 
				&& list.get(size-2).getValue()==Card.CARD_VALUE_JOKER_S ){
			evaluateScore += 8;
		}
		else if ( list.get(size-1).getValue()==Card.CARD_VALUE_JOKER_B ){
			evaluateScore += 4;
		}
		else if ( list.get(size-1).getValue()==Card.CARD_VALUE_JOKER_S ){
			evaluateScore += 3;
		}
		//判断2和A/K的数量
		for (Card card:list){
			switch (card.getValue()){
				case Card.CARD_VALUE_2: evaluateScore += 2; break;
				case Card.CARD_VALUE_A: evaluateScore += 1; break;
				case Card.CARD_VALUE_K: evaluateScore += 1; break;
				default: //do nothing
				break;
			}
		}
		//TODO add bomb count;
		
		Log.d(LOG_TAG, "evaluateScore="+evaluateScore);
		
		if ( evaluateScore > 8){
			return Game.BASIC_SCORE_THREE;
		}
		else if (evaluateScore > 5){
			return Game.BASIC_SCORE_TWO;
		}
		else if (evaluateScore > 3){
			return Game.BASIC_SCORE_ONE;
		}
		else{
			return Game.BASIC_SCORE_NONE;
		}
//		return Game.BASIC_BASIC_SCORE_THREE;
	}
	
	/**
	 * 返回叫牌信息。
	 * @param cards 手牌列表。
	 * @param minScore 最低分（不包含）。这个分数通常是上一个玩家叫的分数。
	 * @return 返回一个比minScore更大的分数，或者 是{@link Game#BASIC_BASIC_SCORE_NONE}，表示不叫。
	 */
	protected final int callLandlord(ArrayList<Card> cards, int minScore) {
		int alalysis = callLandlord(cards);
		return alalysis > minScore ? alalysis : Game.BASIC_SCORE_NONE;
	}
	
	/**
	 * 
	 * 按照指定的pattern尝试组合出目标牌列表。 
	 * 例如传入参数为[3,3,3]，则将尝试找出三张点数为3的卡牌，并把这些按顺序加入一个列表然后返回。
	 * <p>
	 * 注意： <ul>
	 * <li>在找到之后会将这些数据从源列表中删除。</li>
	 * <li>该方法的实现假定list是升序排列的。</li> </ul>
	 * </p>
	 * 
	 * @param targetPattern 目标数值模式。
	 * @param list 当前剩余卡牌列表。
	 * @return 如果能找出这样的卡牌，则返回该列表；否则返回 null。
	 * 如果返回的数值不为null，则相应的数据会从 参数 list 中删除。
	 */
	private ArrayList<Card> takeoutCards(int[] targetPattern, ArrayList<Card> list){
		if (targetPattern==null || list==null){
			Log.w(LOG_TAG, "takecards return null due to null param.");
			return null;
		}
		int patternLength = targetPattern.length;
		int cardLength = list.size();
		if (patternLength > cardLength){
			Log.d(LOG_TAG, "takecards return null due to no enough length.");
			return null;
		}
		ArrayList<Card> targetList = new ArrayList<>();
		//创建临时复制数组，然后从中迭代遍历，如果匹配则加入目标列表，并从临时复制数组中删除迭代的元素。
		//最后检查结果，如果目标列表长度合适则返回，否则返回null。
		ArrayList<Card> internalTempList = new ArrayList<>(list);
		Iterator<Card> iterator = internalTempList.iterator();
		int matchIndex = 0;
		while (iterator.hasNext()){
			Card card = iterator.next();
			if (card.getValue()== targetPattern[matchIndex]){
				targetList.add(card);
				iterator.remove();
				matchIndex++;
				//如果已经到了要出牌的张数，则终止循环
				if (matchIndex == targetPattern.length){
					break;
				}
			}
		}
		//手动清除列表内容，方便GC
		internalTempList.clear();
		Log.d(LOG_TAG, "takecards: pattern="+Arrays.toString(targetPattern)+ ", res="+targetList.toString());
		if (targetList.size() != targetPattern.length){
			targetList.clear();
			return null;
		}
		return targetList;
	}
	
	protected PlayerCardsInfo makeCards(final List<Card> list){
		if (list==null || list.size()==0){
			return null;
		}
		//复制一个列表以便内部操作，避免直接操纵玩家手牌。
		PlayerCardsInfo playerInfo = new PlayerCardsInfo();
		ArrayList<Card> cloneList= new ArrayList<>(list);
		Log.d(LOG_TAG, "First original clone list:"+cloneList.toString());
		Collections.sort(cloneList);
		final int originalLength = cloneList.size();
		//判断王炸是否存在。
		if (cloneList.get(originalLength-1).getValue()==Card.CARD_VALUE_JOKER_B 
			&& cloneList.get(originalLength-1).getValue()==Card.CARD_VALUE_JOKER_B ){
			playerInfo.hasRocket = true;
		}
		
		//因为2和王是不能被连起来的，所以，先把这些排开再连接。
		for (int i=cloneList.size()-1; i>0; i--){//利用排序特性进行优化
			Card card = cloneList.get(i);
			if (card.getValue()==Card.CARD_VALUE_2 || card.getSuit()==CardSuit.Joker){
				playerInfo.twoAndJokerCount++;
				cloneList.remove(card);
			}
		}
		//找出所有的炸弹。通常情况下，炸弹也是不会进行拆分处理的。
		for (int i=0; i<cloneList.size(); i++){
			int cardValue = cloneList.get(i).getValue();
			int[] bombPattern = new int[]{cardValue, cardValue, cardValue, cardValue};
			ArrayList<Card> bombs = takeoutCards(bombPattern, cloneList);
			if (bombs!=null){
				playerInfo.bombCount++;
				playerInfo.cardTypes.add(new Bomb(bombs));
				cloneList.removeAll(bombs);
			}
		}
		Log.d(LOG_TAG, "after remove 2, Joker, and bombs:"+cloneList.toString());
		List<StraightNumbers> numbers = StraightAnalyst.getAllStraights(cloneList);
		Log.d(LOG_TAG, "numbers : "+ numbers.toString());
		ArrayList<Straight> straights = new ArrayList<>();
		for (StraightNumbers num : numbers){
			ArrayList<Card> tempList = takeoutCards(num.asIntegerArray(), cloneList);
			if (tempList != null){
				Straight tempStr = new Straight(tempList);
				straights.add(tempStr);	
			}
		}
		Log.d(LOG_TAG, "final str cards: " + straights.toString());
		return playerInfo;
	}

	private List<ArrayList<CardType>> getAllPatterns(ArrayList<Card> list){
		return null;
	}
}
