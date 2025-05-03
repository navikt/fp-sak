package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdFPGrunnlag;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdFPSak;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdSvpGrunnlag;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdSvpSak;
import no.nav.foreldrepenger.tilganger.AnsattInfoKlient;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SokefeltDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.GrunnlagRequest;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.InfotrygdKode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Periode;
import no.nav.vedtak.felles.integrasjon.infotrygd.saker.v1.respons.InfotrygdSak;
import no.nav.vedtak.felles.integrasjon.person.Persondata;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;

@Path(InfotrygdOppslagRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class InfotrygdOppslagRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InfotrygdOppslagRestTjeneste.class);

    private static final LocalDate FOM = LocalDate.of(2007, 1,1);

    static final String BASE_PATH = "/infotrygd";
    private static final String INFOTRYGD_SOK_PART_PATH = "/sok";
    public static final String INFOTRYGD_SOK_PATH = BASE_PATH + INFOTRYGD_SOK_PART_PATH;

    private Persondata pdlKlient;
    private InfotrygdFPGrunnlag foreldrepenger;
    private InfotrygdSvpGrunnlag svangerskapspenger;

    private InfotrygdFPSak foreldrepengerSak;
    private InfotrygdSvpSak svangerskapspengerSak;
    private AnsattInfoKlient ansattInfoKlient;

    public InfotrygdOppslagRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public InfotrygdOppslagRestTjeneste(Persondata pdlKlient,
                                        AnsattInfoKlient ansattInfoKlient,
                                        InfotrygdFPGrunnlag foreldrepenger,
                                        InfotrygdSvpGrunnlag svangerskapspenger,
                                        InfotrygdFPSak foreldrepengerSak,
                                        InfotrygdSvpSak svangerskapspengerSak) {
        this.pdlKlient = pdlKlient;
        this.ansattInfoKlient = ansattInfoKlient;
        this.foreldrepenger = foreldrepenger;
        this.svangerskapspenger = svangerskapspenger;
        this.foreldrepengerSak = foreldrepengerSak;
        this.svangerskapspengerSak = svangerskapspengerSak;
    }

    @POST
    @Path(INFOTRYGD_SOK_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Søk etter utbetalinger i Infotrygd for fødselsnummer", tags = "infotrygd",
        summary = "Oversikt over utbetalinger knyttet til en bruker kan søkes via fødselsnummer eller d-nummer.",
        responses = {@ApiResponse(responseCode = "200", description = "Returnerer grunnlag",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = InfotrygdVedtakDto.class))}),})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response sokInfotrygd(@TilpassetAbacAttributt(supplierClass = SøkeFeltAbacDataSupplier.class)
        @Parameter(description = "Søkestreng kan være aktørId, fødselsnummer eller D-nummer.") @Valid SokefeltDto søkestreng) {
        var trimmed = søkestreng.getSearchString() != null ? søkestreng.getSearchString().trim() : "";
        var ident = PersonIdent.erGyldigFnr(trimmed) || AktørId.erGyldigAktørId(trimmed) ? trimmed : null;
        if (!PersonIdent.erGyldigFnr(ident)) {
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
        }

        if (!ansattInfoKlient.medlemAvAnsattGruppe(AnsattGruppe.HISTORISK)) {
            LOG.info("FPSAK INFOTRYGD SØK IKKE MEDLEM AV AD-GRUPPE HISTORISK"); // Sjekke bruksfrekvens ...
            throw new FunksjonellException("FP-110706", "Mangler tilgangen Foreldrepenger Historisk", "Be nærmeste leder om tilgangen Foreldrepenger Historisk");
        }

        Set<String> identer = new HashSet<>(finnAlleHistoriskeFødselsnummer(ident));
        identer.add(ident);

        LOG.info("FPSAK INFOTRYGD SØK"); // Sjekke bruksfrekvens ...
        var infotrygdRequest = new GrunnlagRequest(new ArrayList<>(identer), FOM, LocalDate.now());
        List<Grunnlag> grunnlagene = new ArrayList<>();
        // Ser på Dtos senere. Først må vi utforske litt innhold via swagger. Kanskje også fail-hard-utgaven etter litt LOGGING
        grunnlagene.addAll(foreldrepenger.hentGrunnlagFailSoft(infotrygdRequest));
        grunnlagene.addAll(svangerskapspenger.hentGrunnlagFailSoft(infotrygdRequest));
        List<InfotrygdSak> sakene = new ArrayList<>();
        // Ser på Dtos senere. Først må vi utforske litt innhold via swagger. Kanskje også fail-hard-utgaven etter litt LOGGING
        sakene.addAll(foreldrepengerSak.hentSakerFailSoft(infotrygdRequest));
        sakene.addAll(svangerskapspengerSak.hentSakerFailSoft(infotrygdRequest));

        var unikeSaker = sakene.stream().distinct().toList();
        var unikeGrunnlag = grunnlagene.stream().distinct().toList();
        return Response.ok(mapTilVedtakDto(unikeSaker, unikeGrunnlag)).build();
    }

    private List<String> finnAlleHistoriskeFødselsnummer(String inputIdent) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(inputIdent);
        request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT));
        request.setHistorikk(Boolean.TRUE);
        var projection = new IdentlisteResponseProjection()
            .identer(new IdentInformasjonResponseProjection().ident());

        try {
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().map(IdentInformasjon::getIdent).toList();
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                return List.of();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FP-723618", "PDL timeout") : e;
        }
    }

    public static class SøkeFeltAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SokefeltDto) obj;
            var attributter = AbacDataAttributter.opprett();
            var søkestring = req.getSearchString() != null ? req.getSearchString().trim() : "";
            if (søkestring.length() == 13 /* guess - aktørId */) {
                attributter.leggTil(AppAbacAttributtType.AKTØR_ID, søkestring);
            } else if (søkestring.length() == 11 /* guess - FNR */) {
                attributter.leggTil(AppAbacAttributtType.FNR, søkestring);
            }
            return attributter;
        }
    }

    static InfotrygdVedtakDto mapTilVedtakDto(List<InfotrygdSak> saker, List<Grunnlag> grunnlagene) {
        if (saker.isEmpty() && grunnlagene.isEmpty()) {
            return new InfotrygdVedtakDto(List.of(), List.of());
        }
        var mappedSaker = saker.stream()
            .map(s -> new InfotrygdVedtakDto.SakDto(mapSakTermnavn(s.resultat()), s.registrert(), s.saksBlokkNummer(), mapSakTermnavn(s.type()),
                s.vedtatt(), mapSakStønad(s.valg(), s.undervalg()), mapSakTermnavn(s.undervalg()), mapSakTermnavn(s.nivaa())))
            .sorted(Comparator.comparing(InfotrygdVedtakDto.SakDto::registrert))
            .toList();
        var mapped = grunnlagene.stream().map(InfotrygdOppslagRestTjeneste::mapTilVedtakDtoGrunnlag)
            .collect(Collectors.groupingBy(InfotrygdOppslagRestTjeneste::grunnlagKey));
        List<InfotrygdVedtakDto.VedtakKjede> sortertListe = new ArrayList<>();
        mapped.keySet().stream().sorted(Comparator.naturalOrder())
            .map(mapped::get)
            .map(InfotrygdOppslagRestTjeneste::sorterVedtak)
            .filter(l -> !l.isEmpty())
            .map(liste -> new InfotrygdVedtakDto.VedtakKjede(liste.getFirst().identdato(), liste.getFirst().behandlingstema(), liste))
            .forEach(sortertListe::add);
        return new InfotrygdVedtakDto(mappedSaker, sortertListe);
    }

    private static String mapSakStønad(InfotrygdSak.InfotrygdKode valg, InfotrygdSak.InfotrygdKode undervalg) {
        var tvalg = Optional.ofNullable(mapSakTermnavn(valg)).orElse("");
        var tundervalg = Optional.ofNullable(mapSakTermnavn(undervalg))
            .filter(v -> !v.isEmpty() && !v.isBlank())
            .map(v -> tvalg.isEmpty() ? v : " " + v).orElse("");
        return tvalg + tundervalg;
    }

    private static List<InfotrygdVedtakDto.Vedtak> sorterVedtak(List<InfotrygdVedtakDto.Vedtak> vedtak) {
        return Optional.ofNullable(vedtak).orElseGet(List::of).stream()
            .sorted(Comparator.comparing(InfotrygdOppslagRestTjeneste::sortGrunnlagBy))
            .toList();
    }

    private static LocalDate grunnlagKey(InfotrygdVedtakDto.Vedtak grunnlag) {
        return Optional.ofNullable(grunnlag.opprinneligIdentdato())
            .or(() -> Optional.ofNullable(grunnlag.identdato()))
            .or(() -> Optional.ofNullable(grunnlag.registrert()))
            .orElse(FOM);
    }

    private static LocalDate sortGrunnlagBy(InfotrygdVedtakDto.Vedtak grunnlag) {
        return Optional.ofNullable(grunnlag.identdato())
            .or(() -> Optional.ofNullable(grunnlag.registrert()))
            .orElse(FOM);
    }

    private static InfotrygdVedtakDto.Vedtak mapTilVedtakDtoGrunnlag(Grunnlag grunnlag) {
        var utbetaling = mapTilUtbetaling(grunnlag).stream().sorted(Comparator.comparing(p -> p.periode().fom())).toList();
        var arbeidsforhold = mapTilArbeidsforhold(grunnlag);
        var btema = mapKode(grunnlag.behandlingstema());
        var arbKat = mapKode(grunnlag.kategori());
        var dekning = grunnlag.dekningsgrad() != null ? grunnlag.dekningsgrad().prosent() : null;
        var periode = grunnlag.periode() != null ? mapPeriode(grunnlag.periode()) :
            utledPeriode(grunnlag.iverksatt(), grunnlag.opphørFom(), grunnlag.registrert());
        return new InfotrygdVedtakDto.Vedtak(btema, grunnlag.identdato(), grunnlag.opphørFom(), grunnlag.opprinneligIdentdato(), periode,
            grunnlag.registrert(), grunnlag.saksbehandlerId(), arbKat, arbeidsforhold, dekning, grunnlag.fødselsdatoBarn(),
            grunnlag.gradering(), utbetaling);
    }

    private static List<InfotrygdVedtakDto.Arbeidsforhold> mapTilArbeidsforhold(Grunnlag grunnlag) {
        return grunnlag.arbeidsforhold() == null ? List.of() : grunnlag.arbeidsforhold().stream()
            .map(a -> new InfotrygdVedtakDto.Arbeidsforhold(a.orgnr() != null ? a.orgnr().orgnr() : null, a.inntekt(),
                mapKode(a.inntektsperiode()),
                a.refusjon(), a.refusjonTom(), grunnlag.identdato(), grunnlag.opprinneligIdentdato()))
            .toList();
    }

    private static List<InfotrygdVedtakDto.Utbetaling> mapTilUtbetaling(Grunnlag grunnlag) {
        return grunnlag.vedtak() == null ? List.of() : grunnlag.vedtak().stream()
            .map(v -> new InfotrygdVedtakDto.Utbetaling(mapPeriode(v.periode()), v.utbetalingsgrad(), v.arbeidsgiverOrgnr(), v.erRefusjon(),
                v.dagsats(), grunnlag.identdato(), grunnlag.opprinneligIdentdato()))
            .toList();
    }

    private static InfotrygdVedtakDto.Periode mapPeriode(Periode periode) {
        return periode != null ? new InfotrygdVedtakDto.Periode(periode.fom(), periode.tom()) : null;
    }

    private static InfotrygdVedtakDto.Periode utledPeriode(LocalDate iverksatt, LocalDate opphoerFomDato, LocalDate registrert) {
        if (opphoerFomDato != null) {
            LocalDate tomFraOpphørFom = opphoerFomDato.minusDays(1);
            if (tomFraOpphørFom.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue()) {
                tomFraOpphørFom = opphoerFomDato.with(DayOfWeek.FRIDAY);
            }
            if (tomFraOpphørFom.isAfter(iverksatt)) {
                return new InfotrygdVedtakDto.Periode(iverksatt, tomFraOpphørFom);
            } else {
                return new InfotrygdVedtakDto.Periode(iverksatt, iverksatt);
            }
        } else {
            if (iverksatt != null) {
                return new InfotrygdVedtakDto.Periode(iverksatt, Tid.TIDENES_ENDE);
            }
            return new InfotrygdVedtakDto.Periode(registrert, registrert);
        }
    }

    private static InfotrygdVedtakDto.InfotrygdKode mapKode(InfotrygdKode kode) {
        return kode != null ? new InfotrygdVedtakDto.InfotrygdKode(kode.getKode(), kode.termnavn()) : null;
    }

    private static String mapSakTermnavn(InfotrygdSak.InfotrygdKode kode) {
        return kode != null ? kode.termnavn() : null;
    }


}
