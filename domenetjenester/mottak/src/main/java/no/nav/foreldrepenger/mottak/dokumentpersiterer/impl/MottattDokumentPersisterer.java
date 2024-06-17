package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.xml.MottattDokumentXmlParser;
import no.nav.vedtak.exception.TekniskException;

@SuppressWarnings("rawtypes")
@ApplicationScoped
public class MottattDokumentPersisterer {

    private BehandlingEventPubliserer behandlingEventPubliserer;

    MottattDokumentPersisterer() {
    }

    @Inject
    public MottattDokumentPersisterer(BehandlingEventPubliserer behandlingEventPubliserer) {
        this.behandlingEventPubliserer = behandlingEventPubliserer;
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
        behandlingEventPubliserer.publiserBehandlingEvent(new MottattDokumentPersistertEvent(dokument, behandling));
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
