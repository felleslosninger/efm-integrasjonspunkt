package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

@MappedSuperclass
public abstract class AbstractEntity<I extends Serializable> implements Serializable {

    @Id
    @GeneratedValue
    @JsonIgnore
    @XmlTransient
    @Getter
    private I id;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(getClass().isInstance(o))) return false;
        AbstractEntity entity = getClass().cast(o);
        return id != null && id.equals(entity.id);
    }

    @Override
    public final int hashCode() {
        return 31;
    }
}
