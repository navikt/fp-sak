package no.nav.foreldrepenger.jsonfeed;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.HendelseCriteria;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.UtgåendeHendelse;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.jsonfeed.dto.VedtakDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedElement;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.VedtakMetadata;

@ApplicationScoped
public class VedtakFattetTjeneste {
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Oslo");

    private FeedRepository feedRepository;
    private PersoninfoAdapter personinfoAdapter;

    public VedtakFattetTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public VedtakFattetTjeneste(FeedRepository feedRepository, PersoninfoAdapter personinfoAdapter) {
        this.feedRepository = feedRepository;
        this.personinfoAdapter = personinfoAdapter;
    }

    public VedtakDto hentFpVedtak(Long sisteLestSekvensId, Long maxAntall, String hendelseType, Optional<AktørId> aktørId) {
        return hentVedtak(sisteLestSekvensId, maxAntall, hendelseType, aktørId, FpVedtakUtgåendeHendelse.class);
    }

    public VedtakDto hentSvpVedtak(Long sisteLestSekvensId, Long maxAntall, String hendelseType, Optional<AktørId> aktørId) {
        return hentVedtak(sisteLestSekvensId, maxAntall, hendelseType, aktørId, SvpVedtakUtgåendeHendelse.class);
    }

    private <V extends UtgåendeHendelse> VedtakDto hentVedtak(Long sisteLestSekvensId, Long maxAntall, String hendelseType, Optional<AktørId> aktørId,
            Class<V> cls) {
        var builder = new HendelseCriteria.Builder()
                .medSisteLestSekvensId(sisteLestSekvensId)
                .medType(hendelseType)
                .medMaxAntall(maxAntall + 1); // sender med pluss 1 for å få utledet harFlereElementer

        aktørId.ifPresent(it -> builder.medAktørId(it.getId()));

        var hendelseCriteria = builder.build();

        var utgåendeHendelser = feedRepository.hentUtgåendeHendelser(cls, hendelseCriteria);

        var feedElementer = utgåendeHendelser.stream().map(this::mapTilFeedElement).filter(Objects::nonNull)
                .collect(Collectors.toList());
        var harFlereElementer = feedElementer.size() > maxAntall;
        if (harFlereElementer) {
            feedElementer.remove(feedElementer.size() - 1); // Ba om 1 ekstra for å få utledet harFlereElementer, så fjerner den fra
                                                            // outputen
        }

        return new VedtakDto(harFlereElementer, feedElementer);
    }

    private FeedElement mapTilFeedElement(UtgåendeHendelse hendelse) {
        var type = Meldingstype.fromType(hendelse.getType());
        if (type == null) {
            throw new IllegalStateException("Utviklerfeil: Udefinert hendelsetype");
        }

        var innhold = StandardJsonConfig.fromJson(hendelse.getPayload(), type.getMeldingsDto());
        if (innhold.getAktoerId() != null && innhold.getFnr() == null) {
            personinfoAdapter.hentFnr(new AktørId(innhold.getAktoerId())).map(PersonIdent::getIdent).ifPresent(innhold::setFnr);
        }
        return new FeedElement.Builder()
                .medSekvensId(hendelse.getSekvensnummer())
                .medType(hendelse.getType())
                .medInnhold(innhold)
                .medMetadata(new VedtakMetadata.Builder()
                        .medOpprettetDato(ZonedDateTime.of(hendelse.getOpprettetTidspunkt(), ZONE_ID)).build())
                .build();
    }
}
