package no.nav.foreldrepenger.dokumentbestiller.klient;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.TekstFraSaksbehandlerDto;
import no.nav.vedtak.util.StringUtils;

@ApplicationScoped
public class FormidlingDataTjeneste {
    private FormidlingRestKlient formidlingRestKlient;

    public FormidlingDataTjeneste() { // CDI
    }

    @Inject
    public FormidlingDataTjeneste(FormidlingRestKlient formidlingRestKlient) {
        this.formidlingRestKlient = formidlingRestKlient;
    }

    public Optional<TekstFraSaksbehandler> hentSaksbehandlerTekst(UUID behandlingUuid) {
        Optional<TekstFraSaksbehandlerDto> tekstFraSaksbehandlerDtoOptional = formidlingRestKlient
            .hentTekstFraSaksbehandler(new BehandlingUuidDto(behandlingUuid));
        if (tekstFraSaksbehandlerDtoOptional.isPresent()) {
            final TekstFraSaksbehandlerDto tekstFraSaksbehandlerDto = tekstFraSaksbehandlerDtoOptional.get();
            final TekstFraSaksbehandler tekstFraSaksbehandler = new TekstFraSaksbehandler();
            tekstFraSaksbehandler.setAvslagarsakFritekst(tekstFraSaksbehandlerDto.getAvklarFritekst());
            tekstFraSaksbehandler.setVedtaksbrev(utledFormidlingVedtaksbrev(tekstFraSaksbehandlerDto.getVedtaksbrev()));
            tekstFraSaksbehandler.setOverskrift(tekstFraSaksbehandlerDto.getTittel());
            tekstFraSaksbehandler.setFritekstbrev(tekstFraSaksbehandlerDto.getFritekst());

            return Optional.of(tekstFraSaksbehandler);
        } else {
            return Optional.empty();
        }
    }

    private Vedtaksbrev utledFormidlingVedtaksbrev(no.nav.foreldrepenger.kontrakter.formidling.kodeverk.Vedtaksbrev vedtaksbrev) {
        if (vedtaksbrev == null || StringUtils.nullOrEmpty(vedtaksbrev.getKode())) {
            return Vedtaksbrev.UDEFINERT;
        }
        return Vedtaksbrev.fraKode(vedtaksbrev.getKode());
    }
}
