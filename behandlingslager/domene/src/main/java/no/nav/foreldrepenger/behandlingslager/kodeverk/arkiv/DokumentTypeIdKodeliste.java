package no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeliste;

/**
 * DokumentTypeId er et kodeverk som forvaltes av Kodeverkforvaltning. Det er et subsett av kodeverket DokumentType, mer spesifikt alle
 * inngående dokumenttyper.
 * 
 * Denne entiteten brukes kun mot kodeliste tabell for henting av navn
 *
 * @see DokumentType
 */
@Entity(name = "DokumentTypeIdKodeliste")
@DiscriminatorValue(DokumentTypeId.KODEVERK)
public class DokumentTypeIdKodeliste extends Kodeliste implements DokumentType {

    public static final String DISCRIMINATOR = DokumentTypeId.KODEVERK;
    public static final DokumentType UDEFINERT = DokumentTypeId.UDEFINERT;

    DokumentTypeIdKodeliste() {
        // Hibernate trenger en
    }

}
