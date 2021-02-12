package it.unipi.dii.lsdb.group13.guiController;

import it.unipi.dii.lsdb.group13.database.JobOfferDao;
import it.unipi.dii.lsdb.group13.database.JobSeekerDao;
import it.unipi.dii.lsdb.group13.main.Session;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class SavedJobOffersController {

    @FXML
    private Label errorMsg;

    @FXML
    private TableView tableSaved;

    @FXML
    private TableColumn titleCol;

    @FXML
    private void initialize(){
        JobSeekerDao jobSeekerDao = new JobSeekerDao();
        Session.getSingleton();
        ObservableList<String> details = FXCollections.observableArrayList(jobSeekerDao.savedOffers(Session.getLoggedUser()));

        titleCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList<String>, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList<String>, String> p) {
                return new ReadOnlyObjectWrapper(p.getValue());
            }
        });

        tableSaved.setItems(details);

    }

    @FXML
    private void rowsSelected() {
        // show company info??
    }

    @FXML
    private void pressUnsave() {
        String selected = (String) tableSaved.getSelectionModel().getSelectedItem();
        if (selected == null) {
            errorMsg.setText("Please select\na job offer");
            errorMsg.setVisible(true); ;
        } else {
            errorMsg.setVisible(false);
            JobSeekerDao jobSeekerDao = new JobSeekerDao();
            JobOfferDao jobOfferDao = new JobOfferDao();
            System.out.println(jobOfferDao.getJobOffersByCompleteTitle(selected).getId());
            boolean result = jobSeekerDao.unSaveJobOffer(Session.getLoggedUser(), jobOfferDao.getJobOffersByCompleteTitle(selected).getId());
            if(!result) {
                errorMsg.setText("Something went wrong.\nTry again");
            } else {
                initialize();
                errorMsg.setText("Job offer\n" + selected + "\nremoved");
            }
            errorMsg.setVisible(true);
        }

    }
}
