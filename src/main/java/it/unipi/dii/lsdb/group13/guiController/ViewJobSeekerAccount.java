package it.unipi.dii.lsdb.group13.guiController;

import it.unipi.dii.lsdb.group13.database.JobSeekerDao;
import it.unipi.dii.lsdb.group13.entities.JobSeeker;
import it.unipi.dii.lsdb.group13.main.Session;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class ViewJobSeekerAccount {
    @FXML
    private Text firstName;

    @FXML
    private Text lastName;

    @FXML
    private Text gender;

    @FXML
    private Text email;

    @FXML
    private Text birthdate;

    /*
    @FXML
    private Text location;

    @FXML
    private Text skills;
     */

    @FXML
    public void initialize(){
        JobSeekerDao jobSeeker = new JobSeekerDao();
        Session.getSingleton();
        JobSeeker user = jobSeeker.findUser(Session.getLoggedUser());
        firstName.setText("First Name: " + user.getFirstName());
        lastName.setText("Last Name: " + user.getLastName());
        gender.setText("Gender: " + user.getGender());
        email.setText("Email: " + user.getEmail());
        birthdate.setText("BirthDate: " + user.getBirthdate());
    }
}