package no.nav.foreldrepenger.behandlingslager.hendelser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import org.hibernate.annotations.NaturalId;

@Entity(name = "MottattHendelse")
@Table(name = "MOTTATT_HENDELSE")
public class MottattHendelse extends BaseEntitet {

    @Id
    @NaturalId
    @Column(name = "hendelse_uid")
    private String hendelseUid;

    MottattHendelse() {
        //for hibernate
    }

    public MottattHendelse(String hendelseUid) {
        this.hendelseUid = hendelseUid;
    }
}
