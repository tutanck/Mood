package mood.users;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.aj.jeez.servlet.basic.GetServlet;
import org.json.JSONObject;

/**
 * * @author Anagbla Jean */
public class GetProfileServlet extends GetServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public JSONObject doBusiness(HttpServletRequest request, HttpServletResponse response, Map<String, String> params)
			throws Exception {
		//response.getWriter().print(User.getProfile(params));
		return new JSONObject();

	}
		}