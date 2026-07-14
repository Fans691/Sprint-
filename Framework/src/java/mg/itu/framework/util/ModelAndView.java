package mg.itu.framework.util;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String view;
    private Map<String, Object> model = new HashMap<>();

    public ModelAndView(String view) {
        this.view = view;
    }

    public void addObject(String key, Object value){
        model.put(key,value);
    }

    public String getView() {
        return view;
    }

    public Map<String , Object> getModel(){
        return model;
    }
}
