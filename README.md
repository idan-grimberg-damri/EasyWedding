# EasyWedding
This app is the final project I chose to implement as a Computer Science student.

Simulation are in Hebrew. However, there's also support for English.

EasyWedding is an Android app that helps couples who are getting married to organize their wedding.

***Build is in progress.***
  ### Todos
   ##### General
   - Integrate MVVP.
   - Query contacts asynchronously (using java.util.concurrent)
   ##### Chat
   - Push Notification.
   - Message confirmation.
   - Indication of when a user joins the chat or leaves the chat.
   - Sound feedback to actions.
   - Quotation.
   - Send/receive image and pdf. 

### Demos

#### Guests                      
![Guests](demo/6_guests.gif)  

#### Features
![Features](demo/5_features.gif)

##### Chat
![Chat](demo/1_Chat.gif)

#### Shared data - ask for access and grant access
![shared data](demo/7_access.gif)

##### Arrival confirmation (Web) and update in Android
![Arrival confirmation](demo/8_arrival_confirmation.gif)

#### Export guests
![Export guests](demo/8_export_guests.gif)

### App Features
  
    - Multi-User Chat, for wedding content only.
    - Shared data - ask for data access and grant access to data.
    - Arrival confirmation mechanism (Web + Android).
    - Visual indication for guests arrival.
    - Visual indication for payments to suppliers.
    - CRUD operations on guests and features.
    - Delete features by supplier
    - Export guests and features with relevant data.
    - Display the number of arriving guests and their joiners in the action bar. 
    - Varied sort options for features and guests. For example, sort guests such that guests that are (not) arriving displayed first.  
    - Strong data validation on features and guests forms.
    - Support in Url, Phone Number and Email parsing (in chat).
    - Firebase AuthUI sign-in flow.   
    - Supported languages are English and Hebrew.


### Database
 - Firebase Realtime Database 
     
License
----

  Copyright 2020 Idan Damri.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
