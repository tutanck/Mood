package mood.users.welcome;

import fr.aj.jeez.servlet.basic.PostServlet;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Created by Joan on 12/03/2017.
 */
public class SigninServlet extends PostServlet{

    @Override
    public void doBusiness(HttpServletRequest request, HttpServletResponse response, Map<String, String> params)
            throws Exception {
        //JSONObject res=User.logout(params);

    }
}