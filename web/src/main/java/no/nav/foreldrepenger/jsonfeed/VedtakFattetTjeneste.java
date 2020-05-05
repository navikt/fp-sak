package no.nav.foreldrepenger.jsonfeed;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.HendelseCriteria;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.UtgåendeHendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.jsonfeed.dto.VedtakDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedElement;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Innhold;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.VedtakMetadata;

@ApplicationScoped
public class VedtakFattetTjeneste {
    private static final Logger log = LoggerFactory.getLogger(VedtakFattetTjeneste.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Oslo");

    private FeedRepository feedRepository;

    public VedtakFattetTjeneste() {
    }

    @Inject
    public VedtakFattetTjeneste(FeedRepository feedRepository) {
        this.feedRepository = feedRepository;
    }

    public VedtakDto hentFpVedtak(Long sisteLestSekvensId, Long maxAntall, String hendelseType, Optional<AktørId> aktørId) {
        return hentVedtak(sisteLestSekvensId, maxAntall, hendelseType, aktørId, FpVedtakUtgåendeHendelse.class);
    }

    public VedtakDto hentSvpVedtak(Long sisteLestSekvensId, Long maxAntall, String hendelseType, Optional<AktørId> aktørId) {
        return hentVedtak(sisteLestSekvensId, maxAntall, hendelseType, aktørId, SvpVedtakUtgåendeHendelse.class);
    }

    private <V extends UtgåendeHendelse> VedtakDto hentVedtak(Long sisteLestSekvensId, Long maxAntall, String hendelseType, Optional<AktørId> aktørId, Class<V> cls) {
        HendelseCriteria.Builder builder = new HendelseCriteria.Builder()
            .medSisteLestSekvensId(sisteLestSekvensId)
            .medType(hendelseType)
            .medMaxAntall(maxAntall + 1); // sender med pluss 1 for å få utledet harFlereElementer

        aktørId.ifPresent(it -> builder.medAktørId(it.getId()));

        HendelseCriteria hendelseCriteria = builder.build();

        List<V> utgåendeHendelser = feedRepository.hentUtgåendeHendelser(cls, hendelseCriteria);

        List<FeedElement> feedElementer = utgåendeHendelser.stream().map(this::mapTilFeedElement).filter(Objects::nonNull).collect(Collectors.toList());
        boolean harFlereElementer = feedElementer.size() > maxAntall;
        if (harFlereElementer) {
            feedElementer.remove(feedElementer.size() - 1); //Ba om 1 ekstra for å få utledet harFlereElementer, så fjerner den fra outputen
        }

        return new VedtakDto(harFlereElementer, feedElementer);
    }

    private FeedElement mapTilFeedElement(UtgåendeHendelse hendelse) {
        Meldingstype type = Meldingstype.fromType(hendelse.getType());
        if (type == null) { //ignorerer ukjente typer
            throw new IllegalStateException("Utviklerfeil: Udefinert hendelsetype");
        }

        Innhold innhold = JsonMapper.fromJson(hendelse.getPayload(), type.getMeldingsDto());
        return new FeedElement.Builder()
            .medSekvensId(hendelse.getSekvensnummer())
            .medType(hendelse.getType())
            .medInnhold(innhold)
            .medMetadata(new VedtakMetadata.Builder()
                .medOpprettetDato(ZonedDateTime.of(hendelse.getOpprettetTidspunkt(), ZONE_ID)).build())
            .build();
    }
}
