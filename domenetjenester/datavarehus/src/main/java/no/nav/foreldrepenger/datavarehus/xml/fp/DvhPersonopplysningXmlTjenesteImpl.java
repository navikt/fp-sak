package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.DvhPersonopplysningXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.Addresse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.DokumentasjonPeriode;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.FamilieHendelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.Familierelasjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.Inntekt;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.PersonopplysningerDvhForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.RelatertYtelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.Virksomhet;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.YtelseStorrelse;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class DvhPersonopplysningXmlTjenesteImpl extends DvhPersonopplysningXmlTjeneste {

    private final no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory personopplysningBaseObjectFactory = new no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory();
    private final ObjectFactory personopplysningDvhObjectFactory = new ObjectFactory();
    private FamilieHendelseRepository familieHendelseRepository;
    private VergeRepository vergeRepository;
    private MedlemskapRepository medlemskapRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private PersonopplysningXmlFelles personopplysningFellesTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    public DvhPersonopplysningXmlTjenesteImpl() {
        // CDI
    }

    @Inject
    public DvhPersonopplysningXmlTjenesteImpl(PersonopplysningXmlFelles fellesTjeneste,
                                              FamilieHendelseRepository familieHendelseRepository,
                                              VergeRepository vergeRepository,
                                              MedlemskapRepository medlemskapRepository,
                                              VirksomhetTjeneste virksomhetTjeneste,
                                              PersonopplysningTjeneste personopplysningTjeneste,
                                              InntektArbeidYtelseTjeneste iayTjeneste,
                                              YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                              ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        super(personopplysningTjeneste);
        this.personopplysningFellesTjeneste = fellesTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.familieHendelseRepository = familieHendelseRepository;
        this.vergeRepository = vergeRepository;
        this.medlemskapRepository = medlemskapRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    @Override
    public Object lagPersonopplysning(PersonopplysningerAggregat personopplysningerAggregat, Long behandlingId, AktørId aktørId,
                                      Skjæringstidspunkt skjæringstidspunkter) {
        var personopplysninger = personopplysningDvhObjectFactory.createPersonopplysningerDvhForeldrepenger();
        var familieHendelse = personopplysningDvhObjectFactory.createFamilieHendelse();
        personopplysninger.setFamiliehendelse(familieHendelse);

        familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId).ifPresent(familieHendelseGrunnlag -> {
            setAdopsjon(personopplysninger.getFamiliehendelse(), familieHendelseGrunnlag);
            setFødsel(personopplysninger.getFamiliehendelse(), familieHendelseGrunnlag);
            setVerge(behandlingId, personopplysninger);
            setMedlemskapsperioder(behandlingId, personopplysninger);
            setTerminbekreftelse(personopplysninger.getFamiliehendelse(), familieHendelseGrunnlag);
        });
        var skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        setAdresse(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setInntekter(behandlingId, personopplysninger, skjæringstidspunkt);
        setDokumentasjonsperioder(behandlingId, personopplysninger, skjæringstidspunkt);
        setBruker(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setFamilierelasjoner(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setAnnenForelder(personopplysninger, personopplysningerAggregat);
        setRelaterteYtelser(behandlingId, aktørId, personopplysninger, skjæringstidspunkt);

        return personopplysningDvhObjectFactory.createPersonopplysningerDvhForeldrepenger(personopplysninger);
    }

    private void setRelaterteYtelser(Long behandlingId, AktørId aktørId, PersonopplysningerDvhForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);
        var ytelser = ytelseFilter.getFiltrertYtelser();
        if (!ytelser.isEmpty()) {
            var relaterteYtelser = personopplysningDvhObjectFactory
                .createPersonopplysningerDvhForeldrepengerRelaterteYtelser();
            ytelser.forEach(ytelse -> relaterteYtelser.getRelatertYtelse().add(konverterFraDomene(ytelse)));
            personopplysninger.setRelaterteYtelser(relaterteYtelser);
        }
    }

    private RelatertYtelse konverterFraDomene(Ytelse ytelse) {
        var relatertYtelse = personopplysningDvhObjectFactory.createRelatertYtelse();
        relatertYtelse.setKilde(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getKilde()));
        Optional.ofNullable(ytelse.getPeriode())
            .ifPresent(periode -> relatertYtelse.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getFomDato(), periode.getTomDato())));
        Optional.ofNullable(ytelse.getSaksnummer())
            .ifPresent(saksnummer -> relatertYtelse.setSaksnummer(VedtakXmlUtil.lagStringOpplysning(saksnummer.getVerdi())));
        relatertYtelse.setStatus(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getStatus()));
        relatertYtelse.setType(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getRelatertYtelseType()));

        setYtelseAnvist(relatertYtelse, ytelse.getYtelseAnvist());
        setYtelsesgrunnlag(relatertYtelse, ytelse.getYtelseGrunnlag());
        setYtelsesStørrelse(relatertYtelse, ytelse.getYtelseGrunnlag());
        return relatertYtelse;
    }

    private void setYtelsesStørrelse(RelatertYtelse relatertYtelseKontrakt, Optional<YtelseGrunnlag> ytelseGrunnlagDomene) {
        if (ytelseGrunnlagDomene.isPresent()) {
            var ytelseGrunnlag = ytelseGrunnlagDomene.get();
            var ytelseStorrelser = ytelseGrunnlag.getYtelseStørrelse().stream().map(this::konverterFraDomene)
                .toList();
            relatertYtelseKontrakt.getYtelsesstorrelse().addAll(ytelseStorrelser);
        }
    }

    private YtelseStorrelse konverterFraDomene(YtelseStørrelse domene) {
        var kontrakt = personopplysningDvhObjectFactory.createYtelseStorrelse();
        domene.getOrgnr().flatMap(virksomhetTjeneste::finnOrganisasjon)
            .ifPresent(virksomhet -> kontrakt.setVirksomhet(tilVirksomhet(virksomhet)));
        kontrakt.setBeloep(VedtakXmlUtil.lagDecimalOpplysning(domene.getBeløp().getVerdi()));
        kontrakt.setHyppighet(VedtakXmlUtil.lagKodeverksOpplysning(domene.getHyppighet()));
        return kontrakt;
    }

    private Virksomhet tilVirksomhet(no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet domene) {
        var kontrakt = personopplysningDvhObjectFactory.createVirksomhet();
        kontrakt.setNavn(VedtakXmlUtil.lagStringOpplysning(domene.getNavn()));
        kontrakt.setOrgnr(VedtakXmlUtil.lagStringOpplysning(domene.getOrgnr()));
        return kontrakt;
    }

    private void setYtelsesgrunnlag(RelatertYtelse relatertYtelseKontrakt, Optional<YtelseGrunnlag> ytelseGrunnlagDomene) {
        var ytelseGrunnlagOptional = ytelseGrunnlagDomene
            .map(this::konverterFraDomene);
        ytelseGrunnlagOptional.ifPresent(relatertYtelseKontrakt::setYtelsesgrunnlag);
    }

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.YtelseGrunnlag konverterFraDomene(YtelseGrunnlag domene) {
        var kontrakt = personopplysningDvhObjectFactory.createYtelseGrunnlag();
        domene.getArbeidskategori()
            .ifPresent(arbeidskategori -> kontrakt.setArbeidtype(VedtakXmlUtil.lagKodeverksOpplysning(arbeidskategori)));
        domene.getDekningsgradProsent().ifPresent(dp -> kontrakt.setDekningsgradprosent(VedtakXmlUtil.lagDecimalOpplysning(dp.getVerdi())));

        domene.getGraderingProsent()
            .ifPresent(graderingsProsent -> kontrakt.setGraderingprosent(VedtakXmlUtil.lagDecimalOpplysning(graderingsProsent.getVerdi())));
        domene.getInntektsgrunnlagProsent().map(Stillingsprosent::getVerdi)
            .ifPresent(inntektsGrunnlagProsent -> kontrakt.setInntektsgrunnlagprosent(VedtakXmlUtil.lagDecimalOpplysning(inntektsGrunnlagProsent)));

        return kontrakt;
    }

    private void setYtelseAnvist(RelatertYtelse relatertYtelseKontrakt, Collection<YtelseAnvist> ytelseAnvistDomene) {
        var alleYtelserAnvist = ytelseAnvistDomene.stream()
            .map(this::konverterFraDomene).toList();
        relatertYtelseKontrakt.getYtelseanvist().addAll(alleYtelserAnvist);
    }

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.fp.v2.YtelseAnvist konverterFraDomene(YtelseAnvist domene) {
        var kontrakt = personopplysningDvhObjectFactory.createYtelseAnvist();
        domene.getBeløp().ifPresent(beløp -> kontrakt.setBeloep(VedtakXmlUtil.lagDecimalOpplysning(beløp.getVerdi())));
        domene.getDagsats().ifPresent(dagsats -> kontrakt.setDagsats(VedtakXmlUtil.lagDecimalOpplysning(dagsats.getVerdi())));
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(domene.getAnvistFOM(), domene.getAnvistTOM()));
        domene.getUtbetalingsgradProsent().ifPresent(prosent -> kontrakt.setUtbetalingsgradprosent(VedtakXmlUtil.lagDecimalOpplysning(prosent.getVerdi())));

        return kontrakt;
    }

    private void setFamilierelasjoner(Skjæringstidspunkt stp, PersonopplysningerDvhForeldrepenger personopplysninger, PersonopplysningerAggregat aggregat) {
        var aktørPersonopplysningMap = aggregat.getAktørPersonopplysningMap();
        var tilPersoner = aggregat.getSøkersRelasjoner().stream().filter(r -> aktørPersonopplysningMap.get(r.getTilAktørId()) != null).toList();
        if (!tilPersoner.isEmpty()) {
            var familierelasjoner = personopplysningDvhObjectFactory.createPersonopplysningerDvhForeldrepengerFamilierelasjoner();
            personopplysninger.setFamilierelasjoner(familierelasjoner);
            tilPersoner.forEach(relasjon -> personopplysninger.getFamilierelasjoner()
                .getFamilierelasjon()
                .add(lagRelasjon(stp, relasjon, aktørPersonopplysningMap.get(relasjon.getTilAktørId()), aggregat)));
        }
    }

    private void setAnnenForelder(PersonopplysningerDvhForeldrepenger personopplysninger, PersonopplysningerAggregat aggregat) {
        var annenForelder = personopplysningDvhObjectFactory.createAnnenForelder();
        var oppgittAnnenPart = aggregat.getOppgittAnnenPart();
        if (oppgittAnnenPart.isPresent()) {
            oppgittAnnenPart.map(OppgittAnnenPartEntitet::getAktørId).ifPresent(a -> annenForelder.setAktoerId(VedtakXmlUtil.lagStringOpplysning(a.getId())));

            annenForelder.setNavn(VedtakXmlUtil.lagStringOpplysning(oppgittAnnenPart.get().getNavn()));
            if (oppgittAnnenPart.get().getUtenlandskPersonident() != null && !oppgittAnnenPart.get().getUtenlandskPersonident().isEmpty()) {
                annenForelder.setUtenlandskPersonidentifikator(VedtakXmlUtil.lagStringOpplysning(oppgittAnnenPart.get().getUtenlandskPersonident()));
            }
            var landkoder = oppgittAnnenPart.get().getUtenlandskFnrLand();
            if (landkoder != null) {
                annenForelder.setLand(VedtakXmlUtil.lagStringOpplysning(landkoder.getKode()));
            }
            var kodeliste = oppgittAnnenPart.get().getType();
            if (kodeliste != null) {
                annenForelder.setAnnenForelderType(VedtakXmlUtil.lagKodeverksOpplysning(kodeliste));
            }
            personopplysninger.setAnnenForelder(annenForelder);
        }
    }

    private Familierelasjon lagRelasjon(Skjæringstidspunkt stp, PersonRelasjonEntitet relasjon, PersonopplysningEntitet tilPerson, PersonopplysningerAggregat aggregat) {
        var familierelasjon = personopplysningDvhObjectFactory.createFamilierelasjon();
        var person = personopplysningFellesTjeneste.lagUidentifiserbarBruker(stp, aggregat, tilPerson);
        familierelasjon.setTilPerson(person);
        familierelasjon.setRelasjon(VedtakXmlUtil.lagKodeverksOpplysning(relasjon.getRelasjonsrolle()));
        return familierelasjon;
    }

    private void setBruker(Skjæringstidspunkt stp, PersonopplysningerDvhForeldrepenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var person = personopplysningFellesTjeneste.lagUidentifiserbarBruker(stp, personopplysningerAggregat, personopplysningerAggregat.getSøker());
        personopplysninger.setBruker(person);
    }

    private void setDokumentasjonsperioder(Long behandlingId, PersonopplysningerDvhForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {
        var dokumentasjonsperioder = personopplysningDvhObjectFactory
            .createPersonopplysningerDvhForeldrepengerDokumentasjonsperioder();

        foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandlingId).ifPresent(uttak -> {
            var perioder = uttak.getGjeldendePerioder()
                .stream()
                .filter(p -> p.getDokumentasjonVurdering().isPresent())
                .collect(Collectors.toSet());
            for (var periode : perioder) {
                var uttakDokumentasjonType = mapTilDokType(periode.getDokumentasjonVurdering().orElseThrow());
                //Bryr seg bare om perioder der dok er godkjent
                if (uttakDokumentasjonType != null) {
                    var dokPeriode = personopplysningDvhObjectFactory.createDokumentasjonPeriode();
                    dokPeriode.setDokumentasjontype(uttakDokumentasjonType.tilKodeverksOpplysning());
                    dokPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getFom(), periode.getTom()));
                    dokumentasjonsperioder.getDokumentasjonperiode().add(dokPeriode);
                }
            }
        });

        ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId).ifPresent(aggregat -> {
            leggTilPerioderMedAleneomsorg(aggregat, dokumentasjonsperioder);
            if (!aggregat.harOmsorg()) {
                var dokumentasjonPeriode = personopplysningDvhObjectFactory.createDokumentasjonPeriode();
                dokumentasjonPeriode.setDokumentasjontype(UttakDokumentasjonType.UTEN_OMSORG.tilKodeverksOpplysning());
                dokumentasjonPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(skjæringstidspunkt, skjæringstidspunkt.plusYears(3)));
                dokumentasjonsperioder.getDokumentasjonperiode().add(dokumentasjonPeriode);
            }
            if (Boolean.TRUE.equals(aggregat.getAnnenForelderRettAvklaring())) {
                dokumentasjonsperioder.getDokumentasjonperiode()
                    .addAll(lagEnkelDokumentasjonPeriode(UttakDokumentasjonType.ANNEN_FORELDER_HAR_RETT));
            }
            if (Boolean.TRUE.equals(aggregat.getAnnenForelderRettEØSAvklaring())) {
                dokumentasjonsperioder.getDokumentasjonperiode()
                    .addAll(lagEnkelDokumentasjonPeriode(UttakDokumentasjonType.ANNEN_FORELDER_RETT_EOS));
            }
            personopplysninger.setDokumentasjonsperioder(dokumentasjonsperioder);
        });
    }

    static UttakDokumentasjonType mapTilDokType(DokumentasjonVurdering dokumentasjonVurdering) {
        return switch (dokumentasjonVurdering.type()) {
            case SYKDOM_SØKER_GODKJENT -> UttakDokumentasjonType.SYK_SØKER;
            case INNLEGGELSE_SØKER_GODKJENT -> UttakDokumentasjonType.INNLAGT_SØKER;
            case INNLEGGELSE_BARN_GODKJENT -> UttakDokumentasjonType.INNLAGT_BARN;
            case HV_OVELSE_GODKJENT -> UttakDokumentasjonType.HV_OVELSE;
            case NAV_TILTAK_GODKJENT -> UttakDokumentasjonType.NAV_TILTAK;
            case INNLEGGELSE_ANNEN_FORELDER_GODKJENT -> UttakDokumentasjonType.INSTITUSJONSOPPHOLD_ANNEN_FORELDRE;
            case SYKDOM_ANNEN_FORELDER_GODKJENT -> UttakDokumentasjonType.SYKDOM_ANNEN_FORELDER;
            case ALENEOMSORG_GODKJENT -> UttakDokumentasjonType.ALENEOMSORG_OVERFØRING;
            case BARE_SØKER_RETT_GODKJENT -> UttakDokumentasjonType.IKKE_RETT_ANNEN_FORELDER;
            case SYKDOM_SØKER_IKKE_GODKJENT, INNLEGGELSE_SØKER_IKKE_GODKJENT, BARE_SØKER_RETT_IKKE_GODKJENT, ALENEOMSORG_IKKE_GODKJENT,
                SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT, TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT,
                TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT, MORS_AKTIVITET_IKKE_DOKUMENTERT, MORS_AKTIVITET_IKKE_GODKJENT, MORS_AKTIVITET_GODKJENT,
                NAV_TILTAK_IKKE_GODKJENT, HV_OVELSE_IKKE_GODKJENT, INNLEGGELSE_BARN_IKKE_GODKJENT -> null;
        };
    }

    private void leggTilPerioderMedAleneomsorg(YtelseFordelingAggregat aggregat,
                                               PersonopplysningerDvhForeldrepenger.Dokumentasjonsperioder dokumentasjonsperioder) {
        if (aggregat.harAleneomsorg()) {
            dokumentasjonsperioder.getDokumentasjonperiode()
                .addAll(lagEnkelDokumentasjonPeriode(UttakDokumentasjonType.ALENEOMSORG));
        }
    }

    private List<? extends DokumentasjonPeriode> lagEnkelDokumentasjonPeriode(UttakDokumentasjonType dokumentasjonType) {
        var dokumentasjonPeriode = personopplysningDvhObjectFactory.createDokumentasjonPeriode();
        dokumentasjonPeriode.setDokumentasjontype(dokumentasjonType.tilKodeverksOpplysning());
        dokumentasjonPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(LocalDate.now(), LocalDate.now()));
        return List.of(dokumentasjonPeriode);
    }

    private void setInntekter(Long behandlingId, PersonopplysningerDvhForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {
        iayTjeneste.finnGrunnlag(behandlingId).ifPresent(grunnlag -> {
            var aktørInntekt = grunnlag.getAlleAktørInntektFraRegister();
            if (aktørInntekt != null) {
                var inntekterPersonopplysning = personopplysningDvhObjectFactory.createPersonopplysningerDvhForeldrepengerInntekter();
                aktørInntekt.forEach(inntekt -> {
                    var filter = new InntektFilter(inntekt).før(skjæringstidspunkt).filterPensjonsgivende();
                    inntekterPersonopplysning.getInntekt().addAll(lagInntekt(inntekt.getAktørId(), filter));
                    personopplysninger.setInntekter(inntekterPersonopplysning);
                });
            }
        });

    }

    private List<? extends Inntekt> lagInntekt(AktørId aktørId, InntektFilter filter) {
        List<Inntekt> inntektList = new ArrayList<>();

        var inntektXml = personopplysningDvhObjectFactory.createInntekt();
        filter.forFilter((inntekt, inntektsposter) -> {
            var inntektspostXml = personopplysningDvhObjectFactory.createInntektspost();
            if (inntekt.getArbeidsgiver() != null) {
                inntektXml.setArbeidsgiver(VedtakXmlUtil.lagStringOpplysning(inntekt.getArbeidsgiver().getIdentifikator()));
            }
            inntektsposter.forEach(inntektspost -> {
                inntektspostXml.setBeloep(VedtakXmlUtil.lagDoubleOpplysning(inntektspost.getBeløp().getVerdi().doubleValue()));
                inntektspostXml.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato()));
                inntektspostXml.setYtelsetype(VedtakXmlUtil.lagStringOpplysning(inntektspost.getInntektspostType().getKode()));
            });
            inntektXml.getInntektsposter().add(inntektspostXml);
            inntektXml.setMottaker(VedtakXmlUtil.lagStringOpplysning(aktørId.getId()));
        });
        inntektList.add(inntektXml);
        return inntektList;

    }

    private void setTerminbekreftelse(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.TERMIN)) {
            var terminbekreftelseOptional = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse();
            terminbekreftelseOptional.ifPresent(terminbekreftelseFraBehandling -> {
                var terminbekreftelse = personopplysningDvhObjectFactory
                    .createTerminbekreftelse();
                terminbekreftelse.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(familieHendelseGrunnlag.getGjeldendeAntallBarn()));

                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getUtstedtdato())
                    .ifPresent(terminbekreftelse::setUtstedtDato);

                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getTermindato()).ifPresent(terminbekreftelse::setTermindato);

                familieHendelse.setTerminbekreftelse(terminbekreftelse);
            });
        }
    }

    private void setMedlemskapsperioder(Long behandlingId, PersonopplysningerDvhForeldrepenger personopplysninger) {
        medlemskapRepository.hentMedlemskap(behandlingId).ifPresent(medlemskapperioderFraBehandling -> {
            var medlemskap = personopplysningDvhObjectFactory.createMedlemskap();
            personopplysninger.setMedlemskap(medlemskap);

            medlemskapperioderFraBehandling.getRegistrertMedlemskapPerioder()
                .forEach(medlemskapPeriode -> personopplysninger.getMedlemskap().getMedlemskapsperiode()
                    .add(personopplysningFellesTjeneste.lagMedlemskapPeriode(medlemskapPeriode)));
        });
    }

    private void setVerge(Long behandlingId, PersonopplysningerDvhForeldrepenger personopplysninger) {
        vergeRepository.hentAggregat(behandlingId).flatMap(VergeAggregat::getVerge).ifPresent(vergeFraBehandling -> {
            var verge = personopplysningDvhObjectFactory.createVerge();
            verge.setVergetype(VedtakXmlUtil.lagKodeverksOpplysning(vergeFraBehandling.getVergeType()));
            verge.setGyldighetsperiode(VedtakXmlUtil.lagPeriodeOpplysning(vergeFraBehandling.getGyldigFom(), vergeFraBehandling.getGyldigTom()));

            // TODO(PJV): DVH må utvides med de nye feltene for organisasjon (og evt. adresse)

            personopplysninger.setVerge(verge);
        });
    }

    private void setFødsel(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var gjeldendeFamilieHendelse = familieHendelseGrunnlag
            .getGjeldendeVersjon();
        if (Arrays.asList(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN).contains(gjeldendeFamilieHendelse.getType())) {
            var fødsel = personopplysningBaseObjectFactory.createFoedsel();
            fødsel.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(gjeldendeFamilieHendelse.getAntallBarn()));
            gjeldendeFamilieHendelse.getFødselsdato().flatMap(VedtakXmlUtil::lagDateOpplysning).ifPresent(fødsel::setFoedselsdato);
            familieHendelse.setFoedsel(fødsel);
        }
    }

    private void setAdopsjon(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        familieHendelseGrunnlag.getGjeldendeAdopsjon().ifPresent(adopsjonHendelse -> {
            var adopsjon = personopplysningDvhObjectFactory.createAdopsjon();
            if (adopsjonHendelse.getErEktefellesBarn() != null) {
                var erEktefellesBarn = VedtakXmlUtil.lagBooleanOpplysning(adopsjonHendelse.getErEktefellesBarn());
                adopsjon.setErEktefellesBarn(erEktefellesBarn);
            }

            familieHendelseGrunnlag.getGjeldendeBarna().forEach(aBarn -> adopsjon.getAdopsjonsbarn().add(leggTilAdopsjonsbarn(aBarn)));
            familieHendelse.setAdopsjon(adopsjon);
        });
    }

    private Adopsjon.Adopsjonsbarn leggTilAdopsjonsbarn(UidentifisertBarn aBarn) {
        var adopsjonsbarn = personopplysningDvhObjectFactory.createAdopsjonAdopsjonsbarn();
        VedtakXmlUtil.lagDateOpplysning(aBarn.getFødselsdato()).ifPresent(adopsjonsbarn::setFoedselsdato);
        return adopsjonsbarn;
    }

    private void setAdresse(Skjæringstidspunkt stp, PersonopplysningerDvhForeldrepenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var personopplysning = personopplysningerAggregat.getSøker();
        personopplysningerAggregat.getAdresserFor(personopplysning.getAktørId(), SimpleLocalDateInterval.enDag(stp.getUtledetSkjæringstidspunkt()))
            .forEach(adresse -> personopplysninger.getAdresse().add(lagAdresse(adresse)));
    }

    private Addresse lagAdresse(PersonAdresseEntitet adresseFraBehandling) {
        var adresse = personopplysningDvhObjectFactory.createAddresse();
        adresse.setAdressetype(VedtakXmlUtil.lagKodeverksOpplysning(adresseFraBehandling.getAdresseType()));
        adresse.setLand(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getLand()));
        adresse.setPostnummer(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getPostnummer()));
        return adresse;
    }
}
