package com.keysafev2;

/**
 * Created by isqb on 2014-03-06.
 */
import com.appspot.innocreatekey.helloworld.model.HelloGreeting;
import com.appspot.innocreatekey.helloworld.model.DataSet;
import com.appspot.innocreatekey.helloworld.model.Safe;
import com.google.api.client.util.Lists;

import java.util.ArrayList;

/**
 * Dummy Application class that can hold static data for use only in sample applications.
 *
 * TODO(developer): Implement a proper data storage technique for your application.
 */
public class Application extends android.app.Application {
    ArrayList<HelloGreeting> greetings = Lists.newArrayList();


}