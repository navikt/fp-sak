package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.xml.MottattDokumentXmlParser;
import no.nav.foreldrepenger.mottak.publiserer.MottattDokumentPersistertPubliserer;
import no.nav.vedtak.exception.TekniskException;

@SuppressWarnings("rawtypes")
@ApplicationScoped
public class MottattDokumentPersisterer {

    private MottattDokumentPersistertPubliserer publiserer;

    MottattDokumentPersisterer() {
    }

    @Inject
    public MottattDokumentPersisterer(MottattDokumentPersistertPubliserer publiserer) {
        this.publiserer = publiserer;
    }

    public MottattDokumentWrapper xmlTilWrapper(MottattDokument dokument) {
        return MottattDokumentXmlParser.unmarshallXml(dokument.getPayloadXml());
    }

    @SuppressWarnings("unchecked")
    public void persisterDokumentinnhold(MottattDokumentWrapper wrapper,
                                         MottattDokument dokument,
                                         Behandling behandling,
                                         Optional<LocalDate> gjelderFra) {
        MottattDokumentOversetter dokumentOversetter = getDokumentOversetter(wrapper.getSkjemaType());
        dokumentOversetter.trekkUtDataOgPersister(wrapper, dokument, behandling, gjelderFra);
        publiserer.fireEvent(dokument, behandling);
    }

    public void persisterDokumentinnhold(MottattDokument dokument, Behandling behandling) {
        var dokumentWrapper = xmlTilWrapper(dokument);
        persisterDokumentinnhold(dokumentWrapper, dokument, behandling, Optional.empty());
    }

    private MottattDokumentOversetter<?> getDokumentOversetter(String namespace) {
        var annotationLiteral = new NamespaceRef.NamespaceRefLiteral(namespace);

        var instance = CDI.current().select(new TypeLiteralMottattDokumentOversetter(), annotationLiteral);

        if (instance.isAmbiguous()) {
            throw new TekniskException("FP-947148", "Mer enn en implementasjon funnet for skjematype " + namespace);
        }
        if (instance.isUnsatisfied()) {
            throw MottattDokumentFeil.ukjentSkjemaType(namespace);
        }
        var minInstans = instance.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup "
                + "dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    private static final class TypeLiteralMottattDokumentOversetter extends TypeLiteral<MottattDokumentOversetter<?>> {
    }

}
