package fr.aj.jeez.servlet.template;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import fr.aj.jeez.servlet.interfaces.IJEEZServlet;
import fr.aj.jeez.tools.MapRefiner;

/**
 * * @author Anagbla Joan */
public abstract class JEEZServlet
extends HttpServlet
implements IJEEZServlet{
	private static final long serialVersionUID = 1L;

	/**
	 * The set of incoming parameters names required 
	 * for the underlying service to work properly */
	protected Set<String> epnIn=new HashSet<String>(); //Incoming expected parameters names

	/**
	 * The set of outgoing parameters names required 
	 * for the client to work properly */
	protected Set<String> epnOut=new HashSet<String>(); //Outgoing expected parameters names

	/**
	 * The set of incoming additional parameters names  
	 *  taken into account by the underlying service*/
	protected Set<String> opnIn=new HashSet<String>(); //Incoming optional parameters names

	//N'est pas tres important cote server mais pour generer le client , c'est indispensable de savoir l'integalite des noms de params qu'ue servlet peut retourner (pour generer le reviver)
	/**
	 * The set of outgoing additional parameters names  
	 *  taken into account by the underlying service*/
	protected Set<String> opnOut=new HashSet<String>(); //Outgoing optional parameters names


	protected JSONObject beforeBusiness(
			HttpServletRequest request,
			HttpServletResponse response
			)throws IOException {

		response.setContentType("text/plain");

		JSONObject supportedParams= new JSONObject();

		Map<String,String>incomingParams=MapRefiner.refine(request.getParameterMap());

		for(String expected : epnIn) {
			JSONObject res = paramIsValid(incomingParams,expected,supportedParams,true);
			if (!res.getBoolean("valid")){
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL MISUSED");
				return null;
			}
			//update the supported parameters
			supportedParams = (JSONObject) res.get("supportedParams");
		}

		for(String optional : opnIn){
			JSONObject res = paramIsValid(incomingParams,optional,supportedParams,false);
			if (!res.getBoolean("valid")){
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL MISUSED");
				return null;
			}
			//update the supported parameters
			supportedParams = (JSONObject) res.get("supportedParams");
		}

		return supportedParams;
	}


	protected boolean requireToBeConnected(
			HttpServletRequest request,
			HttpServletResponse response,
			boolean require
			)throws IOException {

		boolean succeeded = true;

		HttpSession session = request.getSession(false);

		if(require){
			if(session==null){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "USER UNAUTHENTICATED");
				succeeded=false;
			}
			/*TODO remove this comment : possible to be overwrited by the user 
			 * super.requireToBeConnected()
			 * if (session.getAttribute("ssid_token")==null){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "USER UNAUTHENTICATED");
				return false;}
				##trouver un moyen pour les pb cause par le send de l'http error si cette methode est redefinie
				par l'user idem pour beforeBusiness  : au pire les passer en final */
		}else
			if(session!=null)
				succeeded=false; //Should not have been connected once

		return succeeded;
	}


	protected void afterBusiness(
			HttpServletRequest request, //just a precaution (useless for now)
			HttpServletResponse response,
			JSONObject result,
			boolean debug
			)throws IOException {

		if(!resultWellFormed(result)) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SERVICE CURRENTLY UNAVAILABLE");
			System.err.println("{result} should at least contain all keys in {epnOut}");
			return;
		}
		response.getWriter().print(result);
	}


	//TODO check if it is necessary to check for null or undefined or other
	private JSONObject paramIsValid(
			Map<String,String>incomingParams,
			String typedParameterNameString,
			JSONObject supportedParams,
			boolean strict
			) {
		JSONObject notValid = new JSONObject()
				.put("valid", false)
				.put("supportedParams", supportedParams); //no parameter added

		JSONObject noChanges= new JSONObject()
				.put("valid", true)
				.put("supportedParams", supportedParams); //no parameter added

		//name|string --> {[0]:name(paramName) , [1]:string(paramType)}
		String[] typedParameterNameTab = typedParameterNameString.split("|");
		String paramName = typedParameterNameTab[0];

		//availability test
		if(!incomingParams.containsKey(paramName)
				|| incomingParams.get(paramName).equals(""))
			if (strict)
				return notValid;
			else
				return noChanges;

		//typing test
		if (typedParameterNameTab.length >= 2) {//typedef is provided in the template
			String paramType = typedParameterNameTab[1].trim().toLowerCase();
			try {
				//Copy the supported parameter now typed into a restricted json (contains only typed epn and opn)
				switch (paramType) {
				case "int":
					supportedParams.put(paramName, Integer.parseInt(incomingParams.get(paramName)));
					break;

				case "long":
					supportedParams.put(paramName, Long.parseLong(incomingParams.get(paramName)));
					break;

				case "float":
					supportedParams.put(paramName, Float.parseFloat(incomingParams.get(paramName)));
					break;

				case "double":
					supportedParams.put(paramName, Double.parseDouble(incomingParams.get(paramName)));
					break;

				case "boolean":
					supportedParams.put(paramName, Boolean.parseBoolean(incomingParams.get(paramName)));
					break;

				default:
					supportedParams.put(paramName, incomingParams.get(paramName));
					break;
				}
			} catch (IllegalArgumentException iae) {
				return notValid;
			}
		}else //Copy the supported parameter as string into a restricted json (contains only tped epn and opn)
			supportedParams.put(paramName, incomingParams.get(paramName));

		return new JSONObject()
				.put("valid", true)
				.put("supportedParams", supportedParams); //updated with the valid parameter added
	}


	private boolean resultWellFormed(
			JSONObject result
			){
		boolean resultWellFormed=true;
		for(String expected : epnOut)
			if(!result.has(expected)){
				resultWellFormed=false;
				break;
			}

		return resultWellFormed;
	}

}