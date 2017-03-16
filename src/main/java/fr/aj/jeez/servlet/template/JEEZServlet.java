package fr.aj.jeez.servlet.template;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import fr.aj.jeez.servlet.interfaces.IJEEZServlet;
import fr.aj.jeez.tools.MapRefiner;

/*			TODO	##trouver un moyen pour les pb cause par le send de l'http error si cette methode est redefinie
par l'user idem pour beforeBusiness  : au pire les passer en final */


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

	
	

	/**
	 * @description
	 * -Set the response's content type to 'text/plain'
	 * -Performs some inspection on incoming parameters 
	 * and make sure they fit with the related service requirements/preconditions.
	 * -Send an HTTP error in case of some service's constraint violation.
	 *  
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException */
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



	/**
	 * @description
	 * Check if the result in the server response is sufficient and well formed (
	 * 	contains all needed keys each of the right type (in the json result) 
	 *  to considerate that the result match the service's postconditions.
	 *  )  
	 * @param request
	 * @param response
	 * @param result
	 * @param debug
	 * @throws IOException */
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




	/**
	 * @description
	 * TODO
	 * @param request
	 * @param response
	 * @param requireToBeConnected
	 * @return
	 * @throws IOException */
	protected final boolean requireToBeConnected(
			HttpServletRequest request,
			HttpServletResponse response,
			boolean requireToBeConnected
			)throws IOException {

		if(requireToBeConnected)
			if(!isConnected(request)){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "USER UNAUTHENTICATED");
				return false;
			}	 

		if(!isDisconnected(request)){
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "USER ALREADY AUTHENTICATED");
			return false; 
		}
		return true;
	}

	@Override
	public boolean isConnected(HttpServletRequest request){
		return request.getSession(false)==null;
	}


	@Override
	public boolean isDisconnected(HttpServletRequest request){
		return request.getSession(false)!=null; //Should not have been connected once
	}	


	/**
	 * @description
	 * Check if an incoming parameter is filled (exists and is not empty in the request)
	 * and properly typed according to epnIn and opnIn definitions
	 * @param incomingParams
	 * @param typedParameterNameString
	 * @param supportedParams
	 * @param strict
	 * @return */
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



	/**
	 * @description
	 * Check if the result contains all epnOut's key 
	 * and the corresponding values are properly typed
	 *  
	 * @param result
	 * @return */
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