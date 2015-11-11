package no.difi.virksert.server.repository;

import no.difi.virksert.server.model.Registration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RegistrationRepository {

    @Autowired
    private SessionFactory sessionFactory;

    public Registration findByIdentifier(String identifier) {
        Session session = sessionFactory.openSession();
        Registration result = (Registration) session
                .createCriteria(Registration.class)
                .add(Restrictions.eq("identifier", identifier))
                .uniqueResult();
        session.close();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Registration> listExpiring() {
        Session session = sessionFactory.openSession();
        List<Registration> result = session
                .createCriteria(Registration.class)
                .add(Restrictions.between("expiration", System.currentTimeMillis(), DateTime.now().plusDays(7).getMillis()))
                .addOrder(Order.asc("expiration"))
                .list();
        session.close();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Registration> listRevoked() {
        Session session = sessionFactory.openSession();
        List<Registration> result = session
                .createCriteria(Registration.class)
                .add(Restrictions.gt("revoked", DateTime.now().minusDays(7).getMillis()))
                .addOrder(Order.desc("revoked"))
                .list();
        session.close();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Registration> listUpdated() {
        Session session = sessionFactory.openSession();
        List<Registration> result = session
                .createCriteria(Registration.class)
                .add(Restrictions.gt("updated", DateTime.now().minusWeeks(1).getMillis()))
                .addOrder(Order.desc("updated"))
                .list();
        session.close();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Registration> listValid() {
        Session session = sessionFactory.openSession();
        List<Registration> result = session
                .createCriteria(Registration.class)
                .add(Restrictions.isNull("revoked"))
                .add(Restrictions.gt("expiration", System.currentTimeMillis()))
                .list();
        session.close();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Registration> listAll() {
        Session session = sessionFactory.openSession();
        List<Registration> result = session
                .createCriteria(Registration.class)
                .list();
        session.close();

        return result;
    }

    public void save(Registration registration) {
        Session session = sessionFactory.openSession();
        session.saveOrUpdate(registration);
        session.close();
    }
}
