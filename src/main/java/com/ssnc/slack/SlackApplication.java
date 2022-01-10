package com.ssnc.slack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sds.wallbrain.base.FirewallRuleSessionInfoVo;
import com.sds.wallbrain.base.RuleDiscoveryInfoVo;
import com.sds.wallbrain.ws.client.WallBrainApiConfigHolder;
import com.sds.wallbrain.ws.client.WallBrainRestApiClient;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.event.MessageEvent;
import com.ssnc.slack.config.slackCommonUtil;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.view.Views.*;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.Attachments.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SpringBootApplication
public class SlackApplication {
	private static final Pattern PATTERN_BRACKET = Pattern.compile("(#[a-zA-Z]+)[ ]*\\[([0-9.\\-,/]+)\\]+[ ]*/[ ]*\\[([0-9.\\-,/]+)\\][ ]*/[ ]*\\[([0-9\\-]+)\\]");
	
	private static List<String> findBracketTextByPattern(String text){
        
        ArrayList<String> bracketTextList = new ArrayList<String>();
        Matcher matcher = PATTERN_BRACKET.matcher(text);
       
       
        if(matcher.find()) {
	        for(int i=0; i<=matcher.groupCount();i++) {
	        	 bracketTextList.add(matcher.group(i));
	        }
        }
        
        return bracketTextList;
    }
	
	public static Map<String, Object> callFPMS(String startAdd, String goalAdd, String portNum) throws Exception{

		slackCommonUtil scu = new slackCommonUtil();
		System.out.println("scu.getFPMSRestApiIP() >> "+scu.getFPMSRestApiIP());
		System.out.println("scu.getFPMSRestApiPort() >> "+scu.getFPMSRestApiPort());
		System.out.println("scu.getFPMSRestAPIUser() >> "+scu.getFPMSRestAPIUser());
		System.out.println("scu.getFPMSRestAPIUserpw() >> "+scu.getFPMSRestAPIUserpw());
		
		WallBrainApiConfigHolder configHolder = new WallBrainApiConfigHolder();
		configHolder.setBaseUrl(scu.getFPMSRestApiIP(), Integer.parseInt(scu.getFPMSRestApiPort()));
		configHolder.setUserCredential(scu.getFPMSRestAPIUser(), scu.getFPMSRestAPIUserpw());
		WallBrainRestApiClient client = new WallBrainRestApiClient(configHolder);
		
		boolean withDiscovery = true;
		boolean checkDuplicated = true;
				
		List<FirewallRuleSessionInfoVo> firewallruleSession = client.getSearchProvisionRuleWithPost(startAdd, goalAdd, portNum, withDiscovery, checkDuplicated);
		
		System.out.println(firewallruleSession);
		
		Map<String, Object> map = new HashMap<String, Object>();
		FirewallRuleSessionInfoVo frsiv = firewallruleSession.get(0);
	
		//resultStatus : NOT_FOUND / ALLOWED / NOTALLOWED / INVALD
		String resultStatus = frsiv.getResultStatus();
		
		String codeComment = frsiv.getComplianceComment();
		String comment = "컴플라이언스 위반사항 없음.";
		String code = "000";
		
		if(!(codeComment.equals(""))) {
			comment = "";
			code = "";
			if(codeComment.contains(",")) {
				String commentArr[] = codeComment.split(",");
				for(int i=0;i<commentArr.length;i++) {
					
					//두번째 부터 code 자르고 넣기
					code += commentArr[i].substring(0,3)+" ";
					comment += commentArr[i].substring(3)+" ";
				}
			}else {
				comment = codeComment.substring(3);
				code = codeComment.substring(0,3);
			}
		}
		String earlistExpireDate = frsiv.getEarlistExpireDate();
		boolean isCompliance = frsiv.isCompliance();
		List<RuleDiscoveryInfoVo> firewallPath = frsiv.getFirewallPath();
		

		map.put("srcAddr", frsiv.getSrcAddr());
		map.put("dstAddr", frsiv.getDstAddr());
		map.put("dstPort", frsiv.getDstPort());
		map.put("resultStatus",resultStatus);
		map.put("complianceComment", comment);
		map.put("complianceCode",code);
		map.put("earlistExpireDate", earlistExpireDate);
		map.put("isCompliance", isCompliance);
		map.put("firewallPath", firewallPath);
		//System.out.println(map);
		return map;
	}
	
	public static void main(String[] args) throws Exception {
		slackCommonUtil scu = new slackCommonUtil();
		String signingSecret = scu.getSigningSecret();
		String teamBotToken = scu.getSingleTeamBotToken();
		String channel = scu.getChannel();
		new Thread(() -> {
			
			System.out.println(signingSecret);
			
			AppConfig config = new AppConfig();
			config.setSingleTeamBotToken(teamBotToken);
			config.setSigningSecret(signingSecret);
			var app = new App(config);
						    
			app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
				var appHomeView = view(view -> view
						.type("home")
						.blocks(asBlocks(
								section(section -> section.text(markdownText(mt -> mt.text("연구소 봇 테스트")))),
								divider(),
								section(section -> section.text(markdownText(mt -> mt.text("버튼 테스트")))),
								
								actions(actions -> actions
										.elements(asElements(
												button(b -> b.text(plainText(pt -> pt.text("FPMS test"))).value("FPMS test").actionId("FPMS"))
										))
								)
						))
				);
				
				var res = ctx.client().viewsPublish(r -> r
						.userId(payload.getEvent().getUser())
						.view(appHomeView)
				);

				return ctx.ack();
			});// end of ApphomeOpenedEvenet
			
			app.event(MessageEvent.class, (req, ctx)->{
					//channel = scu.getChannel();
					if(req.getEvent().getText().equals("#FPMS")) { //검증 안내문구 호출
						var result = ctx.client().chatPostMessage(r -> r
			                    .token(ctx.getBotToken())
			                    .channel(channel)
			                    .text("아래 형식에 맞춰 입력하세요\n#Search[srcip]/[dstip]/[port]\n ex) #Search[192.168.1.1]/[8.8.8.8]/[80]")
		                );
						return ctx.ack();
					}
					
					if(req.getEvent().getText().contains("#Search")) { //검증
						
						String str = req.getEvent().getText();
						List<String> list = findBracketTextByPattern(str);
						System.out.println(list);
						
						try {
							System.out.println(list.get(2)+" "+list.get(3)+" "+list.get(4));
							Map<String, Object> map = callFPMS(list.get(2),list.get(3),list.get(4));
							
							List<Object> obj = getContents(map);
							@SuppressWarnings("unchecked")
							var result = ctx.client().chatPostMessage(r -> r
			                    .token(ctx.getBotToken())
			                    .channel(channel)
			                    .blocks((List<LayoutBlock>) obj.get(0))
			                    .attachments((List<Attachment>) obj.get(1))
			                );
						} catch (Exception e) {
							e.printStackTrace();
							var result = ctx.client().chatPostMessage(r -> r
			                    .token(ctx.getBotToken())
			                    .channel(channel)
			                    .text("검증 실패 - 관리자에게 문의 바랍니다.")
			                );
							return ctx.ack();
						}
						return ctx.ack();
					}else {
						var result = ctx.client().chatPostMessage(r -> r
		                    .token(ctx.getBotToken())
		                    .channel(channel)
		                    .text("")
						);
						return ctx.ack();
					}
			});
			
			
			SlackAppServer server = new  SlackAppServer(app);
			try {
				server.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
		SpringApplication.run(SlackApplication.class, args);
	}
	
	public static List<Object> getContents(Map<String, Object> map) throws Exception{
		
		List<Object> returnList = new ArrayList<Object>();
		
		//신청 내용
		String srcAddr = map.get("srcAddr").toString();
		String dstAddr = map.get("dstAddr").toString();
		String dstPort = map.get("dstPort").toString();
		
		//검증 결과
		String complianceCode = map.get("complianceCode").toString();
		String isCompliance = map.get("isCompliance").toString();
		String complianceComment = map.get("complianceComment").toString();
		String resultStatus = map.get("resultStatus").toString(); //중복 여부 allowed
		@SuppressWarnings("unchecked")
		List<RuleDiscoveryInfoVo> firewallPath = (List<RuleDiscoveryInfoVo>) map.get("firewallPath"); //중복 여부 allowed
		
		String requestY = "신청 가능";
		String requestN = "신청 불가능";
		
		String reduplicationY = "기 등록";
		String reduplicationN = "미 등록";
		
		List<LayoutBlock> layoutBlocks = new ArrayList<>();
		List<Attachment> attachments = new ArrayList<>();
		
		layoutBlocks.add(header(header -> header.text(plainText("검증 신청 내용"))));
		layoutBlocks.add(section(section -> section.text(markdownText("srcip ["+srcAddr+"]\ndstip ["+dstAddr+"]\nport ["+dstPort+"]"))));
		layoutBlocks.add(divider());
		
		layoutBlocks.add(section(section -> section.text(markdownText("*FPMS 검증 전체 결과*"))));
		if(resultStatus.equals("allowed")) {
			layoutBlocks.add(section(section -> section.text(markdownText("중복 여부 ["+reduplicationY+"]"))));
			layoutBlocks.add(section(section -> section.text(markdownText("신청 가능 여부 ["+requestN+"]"))));
		}else {
			layoutBlocks.add(section(section -> section.text(markdownText("중복 여부 ["+reduplicationN+"]"))));
			layoutBlocks.add(section(section -> section.text(markdownText("신청 가능 여부 ["+requestY+"]"))));
		}
		
		if(!resultStatus.equals("allowed") && complianceCode.equals("R01")) {		
			layoutBlocks.add(section(section -> section.text(markdownText("신청 가능 여부 ["+requestN+"]"))));
		}
		
		layoutBlocks.add(section(section -> section.text(markdownText("컴플라이언스 여부 ["+isCompliance+":"+complianceComment+"]"))));

		layoutBlocks.add(divider());
		
		layoutBlocks.add(section(section -> section.text(markdownText("*방화벽 별 상세 결과* [총 : "+firewallPath.size()+"]"))));
		
		
		
		//방화벽 별 검증 결과
		for(int i=0;i<firewallPath.size();i++){
			RuleDiscoveryInfoVo infoVo = firewallPath.get(i);
			String ruleStatus = infoVo.getRuleStatus().toString();
			String ruleExpireDay = infoVo.getRuleExpireDay().toString();
			String firewallNm = infoVo.getFirewallId().toString();
			
			attachments.add(attachment(attachment->attachment.title("방화벽 명 ["+firewallNm+"]")));
			
			List<LayoutBlock> block = new ArrayList<>();
			if(ruleStatus.equals("allowed")) {
				block.add(section(section -> section.text(markdownText("중복 여부 ["+reduplicationY+"]\n만료일 ["+ruleExpireDay+"]"))));
			}else {
				block.add(section(section -> section.text(markdownText("중복 여부 ["+reduplicationN+"]\n만료일 ["+ruleExpireDay+"]"))));
			}
			
			infoVo.getRuleComment();
			block.add(divider());
			attachments.add(attachment(attachment->attachment.blocks(block)));
			attachments.add(attachment(attachment->attachment.footer("oasis API")));
		}
	
		returnList.add(layoutBlocks);
		returnList.add(attachments);
		
		return returnList;
	}
}
